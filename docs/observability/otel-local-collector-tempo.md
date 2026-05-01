# OpenTelemetry 本地联调（Docker: Collector + Tempo + Grafana）

本文档用于本地验证以下目标：

1. 应用 trace 可上报到 OTEL Collector，再转发到 Tempo。
2. 能在 Tempo/Grafana 中检索到链路（`workflow.full.*`、`workflow.parse.*`）。
3. 日志里能看到 `traceId/spanId`，并与链路关联。

## 1. 启动本地观测组件

在项目根目录执行：

```bash
docker compose -f docker/observability/docker-compose.yml up -d
docker compose -f docker/observability/docker-compose.yml ps
```

或使用快捷脚本：

```bash
./script/otel-local.sh up
```

组件端口：

- OTEL Collector OTLP HTTP: `4318`
- OTEL Collector OTLP gRPC: `4317`
- Tempo HTTP: `3200`
- Grafana: `3000`（账号 `admin` / 密码 `admin`）

## 2. 启动应用并开启 tracing

本项目已在 `valset-standardizer-boot` 接入 OTEL（Micrometer tracing bridge）。

启动前建议显式设置：

```bash
export OTEL_TRACING_ENABLED=true
export OTEL_SAMPLING_PROBABILITY=1.0
export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://127.0.0.1:4318/v1/traces
```

启动应用（示例）：

```bash
./mvnw -pl valset-standardizer-boot spring-boot:run
```

## 3. 触发链路

最小链路（HTTP server trace）：

```bash
curl -s http://127.0.0.1:30066/actuator/health
```

业务链路（推荐，可看到手工 span）：

1. 调用上传接口：`POST /api/valuation-workflows/upload`
2. 调用解析接口：`POST /api/valuation-workflows/analyze`
3. 或直接调用全流程：`POST /api/valuation-workflows/full-process`

接口示例见：[valuation-workflow-api.md](../valuation-workflow-api.md)

## 4. 在 Grafana/Tempo 验证 trace（目标 2）

1. 打开 `http://127.0.0.1:3000`，登录 `admin/admin`
2. 进入 `Explore`，数据源选择 `Tempo`
3. 按 service name 检索：`valset-standardizer-boot`
4. 查看 span 名称，确认包含：
   - `workflow.full.execute`
   - `workflow.full.extract`
   - `workflow.full.parse`
   - `workflow.full.match`
   - `workflow.parse.execute`
   - `workflow.parse.raw_parse`
   - `workflow.parse.standardize`
   - `workflow.parse.persist_standardized`

## 5. 在日志中验证 traceId 关联（目标 3）

日志模式已包含：

- `traceId=%X{traceId}`
- `spanId=%X{spanId}`

你会在应用日志中看到类似：

```text
[traceId=...,spanId=...] com.yss.valset... - 全流程执行开始...
```

验证方式：

1. 复制日志中的 `traceId`
2. 在 Grafana Tempo Explore 中按 Trace ID 打开
3. 确认与该次请求链路一致

## 6. 停止与清理

```bash
docker compose -f docker/observability/docker-compose.yml down
```

如需连同 Tempo 本地数据一起清理：

```bash
docker compose -f docker/observability/docker-compose.yml down -v
```
