# 文件收发分拣系统设计

## 背景

当前 `tools` 目录已经具备任务调度、解析、知识加载等能力，但缺少一个统一的“文件收发分拣”工具层，无法将来自不同渠道的文件统一收取、识别、分拣并转发。

该工具需要支持以下场景：

- 从指定本地目录收取文件
- 从邮箱服务收取附件文件
- 从 S3 或兼容对象存储收取文件
- 从 SFTP 目录收取文件
- 识别文件后，按照规则转发到其他邮箱、S3 或 SFTP 目标目录
- 支持规则可扩展、插件可扩展
- 支持脚本规则引擎，语法风格类似 QLExpress4

设计目标不是把这些能力拆成多个微服务，而是继续沿用当前项目“模块化单体 + 领域分层 + 轻量持久化调度”的风格，在 `tools` 下形成一个可独立演进的能力模块。

## 设计目标

1. 统一文件入口，屏蔽不同来源的收取差异。
2. 统一分拣引擎，识别逻辑通过插件和脚本规则扩展。
3. 统一投递出口，支持邮箱、S3、SFTP 等目标。
4. 支持幂等处理，避免重复收取、重复分拣、重复转发。
5. 支持配置化、规则化、版本化，便于运维和扩展。
6. 与当前 db-scheduler 调度体系兼容，支持周期扫描和定时转发。

## 模块建议

建议在 `tools` 下新增一个独立模块，例如：

- `tools/transfer`

并沿用当前工具层的分层方式：

- `domain`：领域模型、规则模型、领域服务、网关接口
- `application`：用例编排、命令对象、应用服务
- `infrastructure`：邮件、S3、SFTP、本地目录、数据库、缓存、规则引擎适配器
- `batch`：调度、任务执行器、任务分派

建议 `tools/pom.xml` 增加新模块，但不改变现有模块的职责边界。

## 核心对象

### 1. `TransferSource`

表示一个文件来源配置。

字段建议：

- `sourceId`
- `sourceCode`
- `sourceName`
- `sourceType`
- `enabled`
- `pollCron`
- `connectionConfig`
- `sourceMetaJson`

建议同时为来源配置建立持久化实体 `TransferSourcePO`，表名可使用 `t_transfer_source`，用于保存后台维护的来源配置、轮询表达式和连接参数。

`sourceType` 建议包括：

- `LOCAL_DIR`
- `EMAIL`
- `S3`
- `SFTP`

### 2. `TransferObject`

表示一份已经被收取的文件实体。

字段建议：

- `transferId`
- `sourceId`
- `originalName`
- `extension`
- `mimeType`
- `sizeBytes`
- `fingerprint`
- `sourceRef`
- `localTempPath`
- `status`
- `receivedAt`
- `storedAt`
- `routeId`
- `errorMessage`
- `fileMetaJson`

### 3. `TransferRule`

表示分拣规则。

字段建议：

- `ruleId`
- `ruleCode`
- `ruleName`
- `ruleVersion`
- `enabled`
- `priority`
- `matchStrategy`
- `scriptLanguage`
- `scriptBody`
- `effectiveFrom`
- `effectiveTo`
- `ruleMetaJson`

### 4. `TransferRoute`

表示一次命中的路由结果。

字段建议：

- `routeId`
- `transferId`
- `sourceId`
- `sourceType`
- `sourceCode`
- `ruleId`
- `targetType`
- `targetCode`
- `targetPath`
- `renamePattern`
- `archiveAction`
- `routeStatus`
- `routeMetaJson`

说明：

- 如果路由只用于运行时结果记录，`transferId` 可以作为文件主键。
- 如果同一张表同时承载“路由配置”和“路由结果”，建议把 `sourceId/sourceType/sourceCode` 提升为显式字段，`routeMetaJson` 只放重试、分组、探测结果等扩展信息。
- 目前实现已经按“配置 + 结果共用”方向补齐了来源字段，但如果后续要把配置和结果彻底分离，建议再拆 `t_transfer_route_config`。

`targetType` 建议包括：

- `EMAIL`
- `S3`
- `SFTP`

### 5. `TransferDeliveryRecord`

表示每一次具体投递执行的结果。

字段建议：

- `deliveryId`
- `routeId`
- `transferId`
- `targetType`
- `targetCode`
- `executeStatus`
- `retryCount`
- `requestSnapshot`
- `responseSnapshot`
- `errorMessage`
- `deliveredAt`

### 6. `TransferRunLog`

表示一次调度运行或扫描运行的日志。

字段建议：

- `runId`
- `sourceId`
- `jobId`
- `startAt`
- `endAt`
- `runStatus`
- `processedCount`
- `successCount`
- `failedCount`
- `logMessage`

## 插件化识别设计

识别层建议做成插件体系，而不是把所有判断逻辑写在主流程里。

### 插件分类

#### 1. `ProbePlugin`

负责“看文件是什么”。

能力建议：

- 读取来源元数据
- 判断后缀、MIME、大小
- 读取邮件发件人、主题、附件信息
- 读取 S3 key、etag、bucket、versionId
- 读取 SFTP 路径、文件名、修改时间
- 读取文件头、前几行、压缩包文件清单

#### 2. `MatchPlugin`

负责“这个文件该归谁”。

能力建议：

- 根据规则上下文判断是否命中
- 产生单路由或多路由结果
- 支持优先级、权重、黑白名单
- 支持 fallback 规则

#### 3. `ActionPlugin`

负责“命中后怎么处理”。

能力建议：

- 转发到邮箱
- 上传到 S3
- 上传到 SFTP
- 重命名
- 归档
- 隔离
- 丢弃

### 插件接口建议

```java
public interface FileRecognitionPlugin {

    String type();

    int priority();

    boolean supports(RecognitionContext context);

    RecognitionResult recognize(RecognitionContext context);
}
```

如果希望拆得更清晰，可以进一步拆分为三个接口：

```java
public interface FileProbePlugin {

    String type();

    int priority();

    boolean supports(RecognitionContext context);

    ProbeResult probe(RecognitionContext context);
}

public interface RouteMatchPlugin {

    String type();

    int priority();

    boolean supports(RecognitionContext context);

    MatchResult match(RecognitionContext context, ProbeResult probeResult);
}

public interface TransferActionPlugin {

    String type();

    int priority();

    boolean supports(TransferRoute route);

    TransferResult execute(TransferContext context);
}
```

### 插件注册方式

建议采用“双注册”模式：

- Spring Bean 自动装配，便于内置插件快速接入
- `ServiceLoader` 或约定目录加载，便于外部插件扩展

主引擎只依赖插件抽象，不直接依赖具体实现。

### 插件执行顺序

建议按照以下规则执行：

1. 先按 `priority` 排序
2. 再按 `supports()` 过滤
3. 支持链式执行
4. 支持命中即停
5. 支持多命中合并

## 脚本规则引擎设计

识别规则建议支持脚本化，形式接近 QLExpress4。

### 设计原则

- 规则应可配置
- 规则应可版本化
- 规则应可热更新
- 规则应可回滚
- 规则应只访问受控上下文
- 规则应支持编译缓存

### 规则上下文

脚本不直接面对底层文件句柄，而是通过上下文对象访问已提炼的信息。

```java
public class RecognitionContext {

    private String sourceType;
    private String sourceCode;
    private String fileName;
    private String mimeType;
    private Long fileSize;
    private String sender;
    private String subject;
    private String path;
    private Map<String, Object> attributes;
}
```

建议暴露给脚本的变量包括：

- `sourceType`
- `sourceCode`
- `fileName`
- `mimeType`
- `fileSize`
- `sender`
- `subject`
- `attributes`

### 规则结构

一条规则建议拆成三段：

- `when`：条件表达式
- `then`：命中后的路由动作
- `fallback`：未命中时的兜底动作

示例逻辑：

```text
when:
  sourceType == 'EMAIL' && sender in whitelist && fileName matches '.*\\.xlsx$'

then:
  route to S3 bucket = 'transfer-inbox', prefix = 'incoming/${yyyyMMdd}'

fallback:
  move to quarantine
```

### 规则引擎接口

```java
public interface RuleEngine {

    RuleEvaluationResult evaluate(RuleDefinition ruleDefinition, RuleContext context);
}
```

建议增加适配层：

- `ScriptRuleEngineAdapter`
- `QlexpressRuleExecutor`

这样未来如果切换到其他脚本引擎，主流程不需要重写。

### 规则能力建议

脚本规则建议支持：

- 字符串判断
- 正则判断
- 白名单判断
- 黑名单判断
- 文件扩展名判断
- MIME 类型判断
- 文件大小判断
- 路径前缀判断
- hash 判断
- 组合条件判断
- 路由参数动态拼装

建议内置函数示例：

- `containsIgnoreCase`
- `matchesRegex`
- `isExcel`
- `isCompressed`
- `hashEquals`
- `senderInWhitelist`

## 运行流程

推荐的完整链路如下：

1. 调度器定时触发某个 `TransferSource`
2. 源适配器扫描增量文件
3. 生成 `RecognitionContext`
4. `ProbePlugin` 采集文件特征
5. `RuleEngine` 执行脚本规则
6. 生成一个或多个 `TransferRoute`
7. `ActionPlugin` 执行投递动作
8. 写入 `TransferDeliveryRecord`
9. 写入 `TransferRunLog`
10. 失败文件进入重试、隔离或人工处理队列

## 幂等与去重

建议以 `fingerprint` 作为文件级去重主键。

推荐规则：

- 指纹已存在且成功处理过，默认跳过
- 指纹已存在但处理失败，可按配置重试
- 指纹已存在但业务要求重新投递时，允许生成新路由但保留原始文件记录

建议补充“来源游标”能力：

- 邮件：message-id、uid、附件序号
- S3：bucket、key、etag、versionId
- SFTP：目录、文件名、修改时间、大小
- 本地目录：文件路径、最后修改时间、hash

## 建议表结构

### `t_transfer_source`

保存来源配置。

### `t_transfer_rule`

保存识别和路由规则。

### `t_transfer_object`

保存收取后的文件主数据。

### `t_transfer_route`

保存路由结果。

### `t_transfer_delivery_record`

保存每次投递执行结果。

### `t_transfer_run_log`

保存调度和扫描日志。

### `t_transfer_source_cursor`

保存来源扫描游标，防止重复扫描。

建议字段包括：

- `source_id`
- `cursor_type`
- `cursor_value`
- `updated_at`
- `cursor_meta_json`

## 接口建议

### 来源管理

- `POST /api/transfer/sources`
- `GET /api/transfer/sources`
- `GET /api/transfer/sources/{sourceId}`
- `PUT /api/transfer/sources/{sourceId}`
- `POST /api/transfer/sources/{sourceId}/enable`
- `POST /api/transfer/sources/{sourceId}/disable`

### 规则管理

- `POST /api/transfer/rules`
- `GET /api/transfer/rules`
- `GET /api/transfer/rules/{ruleId}`
- `PUT /api/transfer/rules/{ruleId}`
- `POST /api/transfer/rules/{ruleId}/publish`
- `POST /api/transfer/rules/{ruleId}/rollback`

### 文件与运行记录

- `GET /api/transfer/files`
- `GET /api/transfer/files/{transferId}`
- `GET /api/transfer/files/{transferId}/routes`
- `GET /api/transfer/files/{transferId}/deliveries`
- `GET /api/transfer/runs`
- `GET /api/transfer/runs/{runId}`

### 手动动作

- `POST /api/transfer/files/{transferId}/reprocess`
- `POST /api/transfer/routes/{routeId}/retry`
- `POST /api/transfer/files/{transferId}/quarantine`

## 调度设计

建议继续使用轻量级持久化调度器，当前实现已切换到 db-scheduler，理由是它更贴近 transfer 的“周期扫描 + 立即触发 + 延迟重试”模型，且比 Quartz 更轻。

建议按以下粒度调度：

- 每个来源一个扫描任务
- 每个来源支持独立 cron
- 需要时支持手动立即触发
- 支持对失败任务单独重试

建议仍然保留：

- `TaskDispatcher`
- `TaskExecutor`

但新模块自己的任务类型应独立管理，不和现有估值标准化任务混在一起。

## 目录建议

建议在 `tools/transfer` 下形成如下结构：

```text
tools/transfer
  ├── src/main/java/com/yss/valset/transfer
  │   ├── application
  │   ├── batch
  │   ├── domain
  │   └── infrastructure
  └── src/test/java/com/yss/valset/transfer
```

如果后续模块增多，可以再细分为：

- `transfer-core`
- `transfer-infra`
- `transfer-batch`

但第一期不建议再拆多模块，先保持简单。

## 一期范围

建议第一期只做最小闭环：

- 本地目录收取
- 邮箱收取附件
- S3 和 SFTP 作为投递目标
- 规则插件化
- 脚本规则引擎接入
- db-scheduler 定时扫描
- 投递日志和失败重试

这一阶段不建议同时做：

- 邮件作为投递目标的复杂模板化
- S3/SFTP 作为来源的全量事件监听
- 图形化规则编排器
- 多租户权限体系

## 二期范围

可以在一期稳定后补充：

- S3/SFTP 作为来源
- 邮件作为投递目标
- 多目标 fan-out
- 规则灰度发布
- 规则版本对比
- 文件隔离区管理
- 人工复核台
- 运维监控面板

## 总结

这个文件收发分拣系统建议按“模块化单体 + 插件化识别 + 脚本规则引擎 + db-scheduler 调度”来实现。

核心不是把协议做复杂，而是把边界收清楚：

- 来源适配器负责“收”
- 规则引擎负责“判”
- 动作插件负责“发”
- 数据模型负责“记”
- 调度器负责“跑”

这样后续新增一个邮件服务、一个 S3 桶、一个 SFTP 目录，通常只需要新增配置和插件实现，不需要改主流程。
