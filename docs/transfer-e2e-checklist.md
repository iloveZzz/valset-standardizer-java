# 文件收发分拣验收清单

## 1. 目标

验证来源收取、规则识别、路由生成、目标投递是否能串成闭环。

## 2. 最短路径

1. 创建来源，不在来源表单里配置 `pollCron`
2. 创建规则，脚本返回 `true`
3. 创建目标
4. 创建路由配置并填写 `pollCron`
5. 触发来源执行一次
6. 查询结果

## 3. 触发命令

```bash
curl -X POST "http://localhost:8080/api/transfer-sources/{sourceId}/trigger"
```

## 4. 查询命令

```bash
curl "http://localhost:8080/api/transfer-run-logs?sourceId={sourceId}&limit=20"
curl "http://localhost:8080/api/transfer-objects?sourceCode={sourceCode}&limit=20"
curl "http://localhost:8080/api/transfer-routes?transferId={transferId}"
curl "http://localhost:8080/api/transfer-delivery-records?transferId={transferId}"
```

## 5. 通过标准

- `transfer-run-logs` 先能看到 `INGEST` / `ROUTE` / `DELIVER` 的成功或失败记录
- `transfer-objects` 有新记录，`status=RECEIVED`
- `transfer-routes` 有记录，且 `transferId` 不为空
- `transfer-delivery-records` 有记录，且 `executeStatus=SUCCESS`

## 6. 创建来源示例

```bash
curl -X POST "http://localhost:8080/api/transfer-sources" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceCode": "mail-inbox",
    "sourceName": "邮件收件箱",
    "sourceType": "EMAIL",
    "enabled": true,
    "pollCron": "0 */5 * * * ?",
    "connectionConfig": {
      "protocol": "imap",
      "host": "mail.example.com",
      "port": 993,
      "username": "demo-user",
      "password": "demo-password",
      "folder": "INBOX",
      "ssl": true,
      "startTls": false,
      "limit": 20
    }
  }'
```

## 7. 创建规则示例

```bash
curl -X POST "http://localhost:8080/api/transfer-rules" \
  -H "Content-Type: application/json" \
  -d '{
    "ruleCode": "MAIL_ATTACHMENT_TO_FILESYS",
    "ruleName": "邮件附件投递到文件服务",
    "enabled": true,
    "priority": 10,
    "scriptLanguage": "QLEXPRESS4",
    "scriptBody": "return subject != null && subject.contains(\"测试\");",
    "ruleMeta": {
      "targetType": "FILESYS",
      "targetCode": "filesys-outbox",
      "targetPath": "/transfer/inbox",
      "renamePattern": "${fileName}",
      "maxRetryCount": 3,
      "retryDelaySeconds": 60
    }
  }'
```

## 8. 创建目标示例

```bash
curl -X POST "http://localhost:8080/api/transfer-targets" \
  -H "Content-Type: application/json" \
  -d '{
    "targetCode": "filesys-outbox",
    "targetName": "文件服务投递目标",
    "targetType": "FILESYS",
    "enabled": true,
    "connectionConfig": {
      "parentId": "10001",
      "storageSettingId": "20001",
      "chunkSize": 4194304
    }
  }'
```

## 9. 创建路由示例

```bash
curl -X POST "http://localhost:8080/api/transfer-route-configs" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceCode": "mail-inbox",
    "sourceType": "EMAIL",
    "ruleId": 1,
    "targetType": "FILESYS",
    "targetCode": "filesys-outbox",
    "targetPath": "/transfer/inbox",
    "renamePattern": "${fileName}",
    "routeStatus": "PENDING",
    "routeMeta": {
      "maxRetryCount": 3,
      "retryDelaySeconds": 60
    }
  }'
```

## 10. 失败排查

- 先查 `GET /api/transfer-run-logs?sourceId=...`，确认失败发生在 `INGEST` / `ROUTE` / `DELIVER` 哪一段
- 没有 `transfer-objects`：检查来源、邮箱连通性、附件是否存在、路由 `pollCron`
- 有 `transfer-objects` 但没有 `transfer-routes`：检查规则启用、脚本返回值、目标编码
- 有 `transfer-routes` 但没有 `transfer-delivery-records`：检查目标配置、目标连接器、临时文件路径
