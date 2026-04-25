# 文件收发分拣全流程联调手册

## 执行摘要

最短验证路径：

1. 创建一个启用的邮件来源，带 `pollCron`
2. 创建一个启用的规则，规则脚本返回 `true`
3. 创建一个启用的目标
4. 创建一条路由配置
5. 调用 `POST /api/transfer-sources/{sourceId}/trigger`
6. 查询三类结果：
   - `GET /api/transfer-objects?sourceCode=...`
   - `GET /api/transfer-routes?transferId=...`
   - `GET /api/transfer-delivery-records?transferId=...`

通过判定：

- `transfer-objects` 有新记录且 `status=RECEIVED`
- `transfer-routes` 有命中记录且带 `transferId`
- `transfer-delivery-records` 有投递结果

## 1. 目标

验证文件收发分拣链路是否可以从来源侧自动或手动触发，并完成以下步骤：

1. 按来源配置收取文件或邮件附件
2. 进入规则识别
3. 生成路由结果
4. 按路由投递到目标

当前实现支持两种触发方式：

- 来源配置中的 `pollCron` 定时触发
- 来源详情的手动立即触发

## 2. 前置条件

请先准备以下配置：

- 至少 1 个启用的来源
- 至少 1 个启用的规则
- 至少 1 个启用的目标
- 至少 1 条路由配置，或者一条可命中的脚本规则

如果来源类型是邮件，还需要准备：

- 可访问的 IMAP / IMAPS / POP3 / POP3S 邮箱
- 至少 1 封带附件的邮件

如果目标类型是邮件、S3、SFTP 或 filesys，还需要准备对应可用连接参数。

如果你想先理解来源去重和增量扫描的存储结构，可以先看：

- [来源检查点设计说明](./transfer-checkpoint-design.md)

## 3. 建议的最小配置

### 3.1 来源

建议使用邮件来源，便于验证“附件收取”。

关键字段：

- `sourceType=EMAIL`
- `enabled=true`
- `pollCron=0 */5 * * * ?`
- `connectionConfig.protocol=imap` 或 `imaps`
- `connectionConfig.host`
- `connectionConfig.port`
- `connectionConfig.username`
- `connectionConfig.password`
- `connectionConfig.folder=INBOX`

### 3.2 规则

建议使用脚本规则，脚本只返回布尔值即可先验证链路。

示例逻辑：

- 主题包含某个关键词时命中
- 发件人包含某个域名时命中

### 3.3 目标

建议先用邮件目标或 filesys 目标。

优先推荐 filesys 目标，因为它更容易在接口层确认是否成功写入。

## 4. 触发方式

### 4.1 cron 自动触发

如果来源已经配置了 `pollCron`，保存后会自动注册 db-scheduler 的循环任务。

当 cron 到点时，系统会按以下顺序执行：

1. 收取来源
2. 进入规则识别
3. 生成路由记录
4. 投递目标

### 4.2 手动立即触发

如果你不想等 cron 到点，可以直接调用手动触发接口：

```bash
curl -X POST "http://localhost:8080/api/transfer-sources/{sourceId}/trigger"
```

这个接口会立即执行一次来源收取，然后自动继续后面的规则识别、路由和投递。

## 5. 验证顺序

建议按下面顺序验证：

1. 创建来源
2. 创建规则
3. 创建目标
4. 创建路由配置
5. 调用来源手动触发接口
6. 查看文件主对象
7. 查看路由结果
8. 查看投递记录

## 6. 重点接口

### 6.1 来源

- `POST /api/transfer-sources`
- `PUT /api/transfer-sources/{sourceId}`
- `POST /api/transfer-sources/{sourceId}/trigger`
- `GET /api/transfer-sources`
- `GET /api/transfer-sources/{sourceId}`

### 6.2 路由配置

- `POST /api/transfer-route-configs`
- `PUT /api/transfer-route-configs/{routeId}`
- `GET /api/transfer-route-configs`
- `GET /api/transfer-route-configs/{routeId}`

### 6.3 运行结果查询

- `GET /api/transfer-objects`
- `GET /api/transfer-objects/{transferId}`
- `GET /api/transfer-routes?transferId={transferId}`
- `GET /api/transfer-delivery-records?transferId={transferId}`

## 7. 推荐验证命令

### 7.1 创建来源

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

### 7.2 创建目标

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

### 7.3 创建规则

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

### 7.4 创建路由配置

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

### 7.5 手动触发一次来源

```bash
curl -X POST "http://localhost:8080/api/transfer-sources/1/trigger"
```

## 8. 结果检查

### 8.1 查看文件主对象

```bash
curl "http://localhost:8080/api/transfer-objects?sourceCode=mail-inbox&limit=20"
```

重点看：

- `status` 是否为 `RECEIVED`
- `mailId` / `mailSubject` / `originalName`
- `fileMeta` 里是否有来源和邮件元数据

### 8.2 查看路由记录

```bash
curl "http://localhost:8080/api/transfer-routes?transferId=1"
```

重点看：

- `ruleId`
- `targetType`
- `targetCode`
- `routeStatus`
- `routeMeta` 里的 `ruleMessage` 和 `transferId`

### 8.3 查看投递记录

```bash
curl "http://localhost:8080/api/transfer-delivery-records?transferId=1"
```

重点看：

- `executeStatus`
- `targetType`
- `targetCode`
- `requestSnapshotJson`，现在是请求摘要
- `responseSnapshotJson`，现在是响应摘要

## 9. 最小验收清单

按下面顺序执行，满足对应结果即视为通过：

1. 创建来源，返回结果中 `sourceId` 不为空。
2. 创建来源后查看详情，确认 `pollCron` 已保存。
3. 创建目标，返回结果中 `targetId` 不为空。
4. 创建规则，返回结果中 `ruleId` 不为空。
5. 创建路由配置，返回结果中 `routeId` 不为空。
6. 调用 `POST /api/transfer-sources/{sourceId}/trigger`。
7. 查询 `GET /api/transfer-objects?sourceCode=...`，确认新增 `transferId` 和 `status=RECEIVED`。
8. 查询 `GET /api/transfer-routes?transferId=...`，确认命中规则并生成路由记录。
9. 查询 `GET /api/transfer-delivery-records?transferId=...`，确认存在投递记录。

如果第 7 步成功但第 8 步失败，优先检查规则脚本和启用状态。

如果第 8 步成功但第 9 步失败，优先检查目标连接器和目标配置。

## 10. 预期行为

如果链路正常，应该看到：

1. 来源收取成功后生成 `t_transfer_object` 记录
2. 规则识别命中后生成 `t_transfer_route` 记录
3. 投递执行后生成 `t_transfer_delivery_record` 记录
4. 如果目标投递失败，系统会按 `maxRetryCount` 和 `retryDelaySeconds` 重试

## 11. 常见问题

### 11.1 没有生成文件主对象

优先检查：

- 来源是否启用
- 邮箱连接配置是否正确
- 邮件是否真的有附件
- `pollCron` 是否已注册成功

### 11.2 有文件主对象，但没有路由结果

优先检查：

- 规则是否启用
- 规则脚本是否返回 `true`
- 规则是否已经过有效期
- 规则元数据里的目标编码是否存在

### 11.3 有路由结果，但没有投递记录

优先检查：

- 目标是否启用
- 目标编码是否能查到
- 对应目标连接器是否可用
- 本地临时文件是否还存在

### 11.4 filesys 目标投递失败

优先检查：

- `parentId`
- `storageSettingId`
- `subject.match.filesys.*` 相关配置
- 临时文件路径是否有效
