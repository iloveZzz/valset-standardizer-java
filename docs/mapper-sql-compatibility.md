# Mapper SQL 三库兼容约束

本文约束 `*Mapper.xml` 中的自定义 SQL 写法，目标是同时兼容 MySQL、PostgreSQL、Oracle，并降低长 SQL 的维护成本。

## 职责边界

- Mapper XML 只负责查询原始读模型字段、过滤、排序、分页和必要的状态归一。
- Repository 或 Service 负责展示型字段装配，例如名称回退、时长文案、状态名称、阶段名称。
- `count` 查询应裁剪到计数和过滤必需字段，避免复制完整明细查询。
- 三库差异优先隔离在小型 `<sql>` 片段中，主查询结构尽量复用。

## 方言规则

- 日期格式化：
  - MySQL 使用 `DATE_FORMAT`
  - PostgreSQL、Oracle 使用 `TO_CHAR`
- 时间差：
  - MySQL 使用 `TIMESTAMPDIFF`
  - PostgreSQL 使用 `EXTRACT(EPOCH FROM (...))`
  - Oracle 使用 `EXTRACT(DAY/HOUR/MINUTE/SECOND FROM (...))`
- 分页：
  - MySQL、PostgreSQL 使用 `LIMIT ... OFFSET ...`
  - Oracle 使用 `OFFSET ... ROWS FETCH NEXT ... ROWS ONLY`
- 字符串拼接：
  - MySQL 可使用 `CONCAT`
  - PostgreSQL、Oracle 可使用 `||`
  - 需要三库共用时，放入方言片段，不在主查询中散写。
- JSON 解析优先放到 Java 层处理；确实需要在 SQL 中解析时，必须按三库分别实现。

## 审查清单

- 新增 XML 是否覆盖 MySQL、PostgreSQL、Oracle 三库执行路径。
- PostgreSQL 和 Oracle 是否误用了 MySQL 的 `DATE_FORMAT`、`TIMESTAMPDIFF`、`JSON_EXTRACT`、`CAST(... AS CHAR)`。
- Oracle 是否存在 `NULL` 参数、字符集混用或 `JSON_VALUE` 返回类型风险。
- `count` 查询是否只保留必要字段。
- 展示文案是否被放入 Java 装配层，而不是写进 XML。
