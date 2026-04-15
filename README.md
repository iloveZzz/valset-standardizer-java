# subject-match-java

并行于当前 Python 原型的新 Java 多模块工程，当前已经拆分为“原始数据提取”和“ODS 原始行分析”两段链路。目标技术栈：

- Spring Boot 3
- Quartz
- MyBatis
- Apache POI
- Apache Commons CSV

目录说明：

- `subject-match-core`：领域模型、解析/匹配抽象、任务模型
- `subject-match-tools`：非 DDD 的通用工具库聚合模块
  - `subject-match-extract`：Excel / CSV 原始数据抽取与 `t_ods_valuation_filedata` 持久化
  - `subject-match-analysis`：基于 ODS 原始行数据的估值分析
  - `subject-match-knowledge`：标准科目、历史映射提示和评估样本加载
  - `subject-match-batch`：Quartz 调度与任务分发
- `subject-match-infra`：通用基础设施支持代码
- `subject-match-boot`：启动类、应用服务、控制器

## 当前链路

1. `EXTRACT_DATA` 任务先把 Excel / CSV 按行抽取并落到 ODS 表。
2. `PARSE_WORKBOOK` 和 `MATCH_SUBJECT` 不再直接读 Excel / CSV，而是通过 `fileId` 读取 ODS 原始行数据。
3. `API` / `DB` 数据源继续走原有分析器。
4. `t_subject_match_file_info` 负责文件主数据管理，`file_id` 从文件主表统一发放，`t_subject_match_file_ingest_log` 记录每次接入历史。
5. `t_subject_match_task` 记录流程阶段 `task_stage`，并分别记录 `task_start_time`、`parse_task_time_ms`、`standardize_time_ms`、`match_standard_subject_time_ms`，其中三段耗时分别对应文件解析、标准结构化、标准科目匹配。
6. 同一份文件默认会复用已成功完成的抽取任务、解析任务和匹配任务；如果需要重新执行，可在接口里传入 `forceRebuild=true`。上传返回里会带 `fileFingerprint`，便于排查是否命中同一份文件。

当前版本以“可编译、可运行、可拆分”的多模块实现为目标，便于后续继续扩展分析和匹配能力。

## 全流程接口文档

- 接口调用说明：`docs/valuation-workflow-api.md`
- 数据库初始化说明：`docs/valuation-workflow-db-init.md`
- 文件管理设计：`docs/file-management-design.md`
- 文件管理接口：`/api/files/upload`、`/api/files/{fileId}`、`/api/files`、`/api/files/{fileId}/ingest-logs`、`/api/files/{fileId}/sheet-styles`
