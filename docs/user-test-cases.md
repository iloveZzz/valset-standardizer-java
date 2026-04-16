# 用户测试用例

本文档基于当前 `subject-match-java` 已落地能力整理，覆盖 4 个主流程：

- 原始数据提取
- 估值表解析
- 科目匹配
- 历史映射评估

当前测试默认服务地址：

- `http://localhost:30066`

当前示例文件：

- 估值表：`/Users/zhudaoming/yss-subject-match/input/20230321基金资产估值表DJ0233.xls`
- 标准科目：`/Users/zhudaoming/yss-subject-match/standard/光大标准科目（解密后）.xlsx`
- 历史映射：`/Users/zhudaoming/yss-subject-match/standard/光大科目映射（解密后）.xlsx`

## 1. 测试前准备

1. 启动 `subject-match-java` 服务。
2. 确认端口 `30066` 可访问。
3. 确认上述 3 个样例文件路径在当前机器存在。

## 2. 用例 1：原始数据提取成功

目的：
验证 `/api/tasks/extract` 能正确创建提取任务，并把 Excel / CSV 行数据落到 ODS 表。

请求：

```bash
curl -X POST http://localhost:30066/api/tasks/extract \
  -H 'Content-Type: application/json' \
  -d '{
    "workbookPath": "/Users/zhudaoming/yss-subject-match/input/20230321基金资产估值表DJ0233.xls",
    "createdBy": "uat-user"
  }'
```

预期：

- 返回 `taskId`
- `taskType` 为 `EXTRACT_DATA`
- `taskStatus` 初始为 `PENDING` 或 `RUNNING`

轮询任务：

```bash
curl http://localhost:30066/api/tasks/{taskId}
```

验收点：

- 最终 `taskStatus=SUCCESS`
- `resultPayload` 中包含 `rowCount`、`fileSizeBytes`、`durationMs`
- `rowCount` 与 `t_ods_valuation_filedata` 中对应 `task_id` 的行数一致
- `resultPayload.outputDir` 存在
- 可记录本次提取得到的 `fileId`，供后续解析和匹配任务使用

## 3. 用例 2：解析估值表成功

目的：
验证 `/api/tasks/parse` 能正确创建解析任务，并输出解析结果文件。

请求：

```bash
curl -X POST http://localhost:30066/api/tasks/parse \
  -H 'Content-Type: application/json' \
  -d '{
    "workbookPath": "/Users/zhudaoming/yss-subject-match/input/20230321基金资产估值表DJ0233.xls",
    "fileId": 42,
    "createdBy": "uat-user"
  }'
```

预期：

- 返回 `taskId`
- `taskType` 为 `PARSE_WORKBOOK`
- `taskStatus` 初始为 `PENDING` 或 `RUNNING`

轮询任务：

```bash
curl http://localhost:30066/api/tasks/{taskId}
```

验收点：

- 最终 `taskStatus=SUCCESS`
- `resultPayload` 非空
- `inputData.workbookPath` 与请求一致
- `resultData.outputDir`、`resultData.artifacts` 可直接读取
- 产物目录存在：`/Users/zhudaoming/yss-subject-match/subject-match-java/output/task-{taskId}` 或运行配置里的输出目录
- 目录中至少包含：
    - `parsed.json`
    - `subjects.csv`
    - `subject_relations.csv`
    - `subject_tree.json`
    - `metrics.csv`
    - `summary.json`
    - `parsed.duckdb`

重点核查：

- `subjects.csv` 中科目数量大于 0
- `summary.json` 中 `subjectCount`、`maxLevel` 有值

## 4. 用例 3：标准科目匹配成功

目的：
验证 `/api/tasks/match` 能完成估值表到标准科目的规则匹配，并输出候选与复核队列。

请求：

```bash
curl -X POST http://localhost:30066/api/tasks/match \
  -H 'Content-Type: application/json' \
  -d '{
    "workbookPath": "/Users/zhudaoming/yss-subject-match/input/20230321基金资产估值表DJ0233.xls",
    "fileId": 42,
    "standardWorkbookPath": "/Users/zhudaoming/yss-subject-match/standard/光大标准科目（解密后）.xlsx",
    "mappingWorkbookPath": "/Users/zhudaoming/yss-subject-match/standard/光大科目映射（解密后）.xlsx",
    "topK": 5,
    "createdBy": "uat-user"
  }'
```

预期：

- 返回 `taskType=MATCH_SUBJECT`
- 查询任务后最终状态为 `SUCCESS`

验收点：

- 输出目录中包含：
    - `match_results.json`
    - `match_summary.json`
    - `match_top1.csv`
    - `match_candidates.csv`
    - `review_queue.csv`
    - `match.duckdb`

重点核查：

- `match_top1.csv` 行数大于 0
- `match_candidates.csv` 中每个外部科目最多保留 `topK` 个候选
- `match_summary.json` 中 `subject_count` 大于 0
- 查询任务返回的 `resultData.reviewQueueCount`、`resultData.highConfidenceCount`、`resultData.outputDir` 有值
- `review_queue.csv` 存在待人工复核记录时，`review_status` 默认值为 `PENDING_REVIEW`

建议抽查：

- 抽 5 条 `match_top1.csv`，确认 `matched_standard_code` 和 `matched_standard_name` 已填充
- 抽 3 条 `review_queue.csv`，确认低置信或边界样本已进入复核队列

## 5. 用例 4：历史映射离线评估成功

目的：
验证 `/api/tasks/evaluate` 能完成训练/测试切分、基线评估、调权搜索和失败分析。

请求：

```bash
curl -X POST http://localhost:30066/api/tasks/evaluate \
  -H 'Content-Type: application/json' \
  -d '{
    "mappingWorkbookPath": "/Users/zhudaoming/yss-subject-match/standard/光大科目映射（解密后）.xlsx",
    "standardWorkbookPath": "/Users/zhudaoming/yss-subject-match/standard/光大标准科目（解密后）.xlsx",
    "splitMode": "org_holdout",
    "topK": 5,
    "maxTuningSamples": 1500,
    "createdBy": "uat-user"
  }'
```

预期：

- 返回 `taskType=EVALUATE_MAPPING`
- 查询任务后最终状态为 `SUCCESS`

验收点：

- 输出目录中包含：
    - `mapping_evaluation.json`
    - `failure_cluster.json`
    - `weight_search_report.json`
    - `evaluation.duckdb`

重点核查：

- `mapping_evaluation.json` 中包含：
    - `train_sample_count`
    - `test_sample_count`
    - `baseline_metrics`
    - `recommended_metrics`
    - `weight_search`
    - `failure_analysis`
- 查询任务返回的 `resultData.trainSampleCount`、`resultData.testSampleCount`、`resultData.recommendedTop1` 有值
- `recommended_metrics.top1_accuracy` 不低于 `baseline_metrics.top1_accuracy`
- `evaluation.duckdb` 中表存在：
    - `evaluation_summary`
    - `weight_versions`
    - `failure_clusters`
    - `failure_samples`
    - `weight_search_report`

## 6. 用例 5：解析入参缺失

目的：
验证必填参数校验生效。

请求：

```bash
curl -X POST http://localhost:30066/api/tasks/parse \
  -H 'Content-Type: application/json' \
  -d '{
    "createdBy": "uat-user"
  }'
```

预期：

- 返回 4xx
- 响应提示 `workbookPath` 缺失或校验失败

## 7. 用例 6：匹配文件路径错误

目的：
验证任务执行异常时能正确失败，而不是假成功。

请求：

```bash
curl -X POST http://localhost:30066/api/tasks/match \
  -H 'Content-Type: application/json' \
  -d '{
    "workbookPath": "/tmp/not-exists.xls",
    "standardWorkbookPath": "/Users/zhudaoming/yss-subject-match/standard/光大标准科目（解密后）.xlsx",
    "topK": 5,
    "createdBy": "uat-user"
  }'
```

验收点：

- 任务会被创建
- 查询任务后最终 `taskStatus=FAILED`
- `resultData.errorMessage` 或 `resultPayload` 中能看出是文件不存在/读取失败

## 8. 用例 7：评估切分模式切换

目的：
验证两种评估切分模式都可运行。

请求 1：

```bash
curl -X POST http://localhost:30066/api/tasks/evaluate \
  -H 'Content-Type: application/json' \
  -d '{
    "mappingWorkbookPath": "/Users/zhudaoming/yss-subject-match/standard/光大科目映射（解密后）.xlsx",
    "standardWorkbookPath": "/Users/zhudaoming/yss-subject-match/standard/光大标准科目（解密后）.xlsx",
    "splitMode": "org_holdout",
    "createdBy": "uat-user"
  }'
```

请求 2：

```bash
curl -X POST http://localhost:30066/api/tasks/evaluate \
  -H 'Content-Type: application/json' \
  -d '{
    "mappingWorkbookPath": "/Users/zhudaoming/yss-subject-match/standard/光大科目映射（解密后）.xlsx",
    "standardWorkbookPath": "/Users/zhudaoming/yss-subject-match/standard/光大标准科目（解密后）.xlsx",
    "splitMode": "hash_holdout",
    "createdBy": "uat-user"
  }'
```

验收点：

- 两个任务最终都能 `SUCCESS`
- 两次 `mapping_evaluation.json` 中 `split_mode` 不同
- 两次 `train_sample_count` / `test_sample_count` 允许不同，但都必须大于 0

## 9. 建议的人工验收结论模板

可按下面模板记录：

```text
测试日期：
测试人：
服务版本：

用例 1 解析估值表：通过 / 不通过
用例 2 标准科目匹配：通过 / 不通过
用例 3 历史映射评估：通过 / 不通过
用例 4 解析入参缺失：通过 / 不通过
用例 5 匹配文件路径错误：通过 / 不通过
用例 6 评估切分模式切换：通过 / 不通过

问题记录：
1.
2.
3.
```

## 9. 一键冒烟脚本

如果希望一次性完成解析、匹配、两种评估模式的基本验证，可以直接执行：

```bash
bash /Users/zhudaoming/yss-subject-match/scripts/run_subject_match_user_smoke.sh
```

可选环境变量：

- `BASE_URL`
- `VALUATION_WORKBOOK`
- `STANDARD_WORKBOOK`
- `MAPPING_WORKBOOK`
- `CREATED_BY`
- `POLL_INTERVAL_SECONDS`
- `POLL_TIMEOUT_SECONDS`

示例：

```bash
BASE_URL=http://localhost:30066 \
POLL_TIMEOUT_SECONDS=300 \
bash /Users/zhudaoming/yss-subject-match/scripts/run_subject_match_user_smoke.sh
```
