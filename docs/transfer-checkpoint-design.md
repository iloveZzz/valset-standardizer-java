# 来源检查点设计说明

## 1. 目标

统一来源去重和增量扫描的运行态存储方式，覆盖以下来源：

- `EMAIL`
- `LOCAL_DIR`
- `S3`
- `SFTP`

当前方案将“配置”和“运行态”拆开：

- 来源配置仍然保存在 `t_transfer_source`
- 大量去重记录保存在 `t_transfer_source_checkpoint_item`
- 轻量扫描游标保存在 `t_transfer_source_checkpoint`

## 2. 为什么要拆表

如果把大量去重记录继续放在来源表里，会有这些问题：

- JSON 会持续膨胀
- 每次读取都要解析整段内容
- 每次写回都要重写整段 JSON
- 并发下更容易产生锁竞争
- 语义上会把“配置”与“运行态去重”混在一起

## 3. 表职责

### 3.1 `t_transfer_source`

只保留来源配置字段：

- `source_code`
- `source_name`
- `source_type`
- `enabled`
- `poll_cron`
- `connection_config_json`
- `source_meta_json`

### 3.2 `t_transfer_source_checkpoint`

用于保存轻量扫描游标和运行态状态。

当前主要用途：

- `scanCursor`

表内推荐字段：

- `source_id`
- `source_type`
- `checkpoint_key`
- `checkpoint_value`
- `checkpoint_json`
- `created_at`
- `updated_at`

唯一约束：

- `(source_id, checkpoint_key)`

### 3.3 `t_transfer_source_checkpoint_item`

用于保存已处理条目的去重记录。

表内推荐字段：

- `source_id`
- `source_type`
- `item_key`
- `item_ref`
- `item_name`
- `item_size`
- `item_mime_type`
- `item_fingerprint`
- `item_meta_json`
- `trigger_type`
- `processed_at`

唯一约束：

- `(source_id, item_key)`

## 4. `item_key` 规范

### 4.1 EMAIL

邮件来源的 `item_key` 采用邮件唯一标识：

1. 优先使用 `Message-ID`
2. 如果没有 `Message-ID`，且支持 `UIDFolder`，则使用 `protocol:folder:uid`
3. 再没有时，退回 `protocol:folder:messageNumber`

对应当前实现里的 `mailId`。

### 4.2 LOCAL_DIR

本地目录来源的 `item_key` 采用：

- `absolutePath|lastModified|size`

这样即使同名文件被替换，也可以通过时间和大小区分。

### 4.3 S3

S3 来源的 `item_key` 采用：

- `bucket|objectKey|etag|size|lastModified`

如果对象存储支持版本号，也可以在后续扩展时加入版本字段。

### 4.4 SFTP

SFTP 来源的 `item_key` 采用：

- `remotePath|size|mtime`

如果远程目录中的文件会被覆盖更新，这个组合可以较稳定地区分文件版本。

## 5. `scanCursor` 语义

`scanCursor` 是“从哪里继续扫”的轻量游标，不是完整的历史去重列表。

当前行为：

1. 收取前先读取该来源最近一次保存的 `scanCursor`
2. 扫描时先跳过游标之前的对象
3. 扫到游标对应对象后，开始处理后续对象
4. 真正是否重复，仍然以 `t_transfer_source_checkpoint_item` 为准

这意味着：

- `scanCursor` 负责缩小扫描范围
- `checkpoint_item` 负责最终去重

## 6. 写入时机

### 6.1 运行时去重记录

每个成功进入收取链路的对象，都会写入一条 `checkpoint_item`。

### 6.2 扫描游标

每次成功收取并生成主对象后，会写入一条 `scanCursor` 检查点。

对应的 `checkpoint_key` 固定为：

- `scanCursor`

## 7. 清理行为

当来源被清空检查点或删除时，会同时清理：

- `t_transfer_source_checkpoint_item`
- `t_transfer_source_checkpoint`

旧的来源表检查点字段已经下线，不再作为业务状态来源。

## 8. 联调时的检查方式

### 8.1 查游标

```sql
select *
from t_transfer_source_checkpoint
where source_id = '你的sourceId';
```

重点看：

- `checkpoint_key = scanCursor`
- `checkpoint_value` 是否是最近处理到的对象标识

### 8.2 查去重记录

```sql
select *
from t_transfer_source_checkpoint_item
where source_id = '你的sourceId'
order by processed_at desc;
```

重点看：

- `item_key`
- `item_ref`
- `item_name`
- `processed_at`

### 8.3 查来源配置

```sql
select source_id, source_code, source_type, connection_config_json, source_meta_json
from t_transfer_source
where source_id = '你的sourceId';
```

重点看：

- `connection_config_json` 是否配置正确
- `source_meta_json` 是否符合当前来源需要

### 8.4 查 API

```bash
curl "http://localhost:8080/api/transfer-sources/{sourceId}/checkpoints?limit=20"
curl "http://localhost:8080/api/transfer-sources/{sourceId}/checkpoint-items?limit=20"
```

这两个接口分别返回：

- 当前来源的游标记录
- 当前来源的处理明细记录

## 9. 当前实现的注意事项

- `checkpoint_item` 是最终去重依据
- `scanCursor` 只负责缩小扫描范围
- 如果游标记录丢失，来源仍然可以靠 `checkpoint_item` 兜底
- 如果去重表被清空，来源会重新扫描，但不会影响配置本身

## 10. 后续可扩展点

后续如果要进一步优化增量扫描，可以把每种来源的游标拆成更细的类型，例如：

- EMAIL 的 UID 游标
- LOCAL 的目录游标
- S3 的分页 token 或对象位点
- SFTP 的目录快照游标

当前版本先统一使用 `scanCursor`，便于联调和后续扩展。
