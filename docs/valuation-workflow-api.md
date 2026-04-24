# 外部估值全流程接口说明

## 概览

这组接口用于串联外部估值表处理全流程：

1. 上传外部估值表
2. 抽取 ODS 原始行数据
3. 基于 ODS 原始行生成 STG 解析数据，并进一步生成 DWD 标准化数据
4. 将外部估值明细与内部标准科目做匹配打标
5. 查询 ODS、STG、DWD 和匹配结果

接口前缀：`/api/valuation-workflows`

## 1. 上传并抽取 ODS 原始数据

`POST /api/valuation-workflows/upload`

请求类型：`multipart/form-data`

字段：

- `file`: 必填，外部估值文件
- `dataSourceType`: 可选，`EXCEL` 或 `CSV`
- `createdBy`: 可选，发起人
- `forceRebuild`: 可选，默认 `false`。`false` 时如果同一份文件已经成功抽取过，则直接复用已有结果；`true` 时强制重新生成抽取任务并重新落 ODS。

示例：

```bash
curl -X POST "http://localhost:8080/api/valuation-workflows/upload" \
  -F "file=@./input/20230321基金资产估值表DJ0233.xls" \
  -F "dataSourceType=EXCEL" \
  -F "createdBy=workflow-user"
```

返回重点字段：

- `fileId`: 后续分析、匹配、查询都使用这个标识，来源于 `t_subject_match_file_info`
- `workbookPath`: 仅用于本次抽取的临时落盘路径，抽取完成后会清理
- `fileFingerprint`: 文件内容指纹，同一份文件再次上传会复用该指纹对应的成功任务
- `filesysTaskId`: `yss-filesys-feignsdk` 创建的上传任务标识
- `filesysFileId`: `yss-filesys-feignsdk` 返回的文件标识
- `filesysObjectKey`: `yss-filesys-feignsdk` 返回的对象键
- `filesysInstantUpload`: 是否命中 instant upload
- `extractTask`: 抽取任务结果
- `reusedExistingExtractTask`: 是否复用了已有的抽取任务
- `extractTask.taskStage`: 任务阶段，固定为 `EXTRACT`
- `extractTask.taskStartTime`: 任务开始时间
- `extractTask.parseTaskTimeMs`: 文件解析耗时

说明：

- 现在也可以直接调用 `POST /api/files/upload` 完成同样的文件接入和 ODS 提取流程。
- filesys 上传依赖 `subject.match.filesys.parent-id` 与 `subject.match.filesys.storage-setting-id`，如果未配置会退回为仅生成本地临时抽取副本。

## 2. 生成 DWD 外部估值标准数据

`POST /api/valuation-workflows/analyze`

请求类型：`application/json`

```json
{
  "dataSourceType": "EXCEL",
  "workbookPath": "./uploads/2026-04-14/8d3e3f9f_demo.xls",
  "fileId": 10001,
  "createdBy": "workflow-user"
}
```

说明：

- `fileId` 必填
- `workbookPath` 仍然保留在任务入参里，便于任务追踪和结果输出
- `forceRebuild` 可选，默认 `false`。开启后会强制重新生成解析任务
- 该接口会先写入 STG 解析表，再写入 DWD 标准表
- 返回的任务结果会包含：
  - `taskStage=PARSE`
  - `taskStartTime`
  - `standardizeTimeMs`

## 3. 执行外部估值科目匹配

`POST /api/valuation-workflows/match`

请求类型：`application/json`

```json
{
  "dataSourceType": "EXCEL",
  "workbookPath": "./uploads/2026-04-14/8d3e3f9f_demo.xls",
  "fileId": 10001,
  "standardWorkbookPath": "./standard/光大标准科目（解密后）.xlsx",
  "mappingWorkbookPath": "./standard/光大科目映射（解密后）.xlsx",
  "topK": 5,
  "createdBy": "workflow-user"
}
```

说明：

- `fileId` 必填
- 匹配时优先读取 DWD 标准表
- 如果 DWD 未生成，系统会回退到当前解析器逻辑
- `forceRebuild` 可选，默认 `false`。开启后会强制重新生成匹配任务
- 返回的任务结果会包含：
  - `taskStage=MATCH`
  - `taskStartTime`
  - `matchStandardSubjectTimeMs`

## 4. 一次跑完整链路

`POST /api/valuation-workflows/full-process`

请求类型：`multipart/form-data`

字段：

- `file`: 必填，外部估值文件
- `standardWorkbookPath`: 必填，内部标准科目表路径
- `mappingWorkbookPath`: 可选，历史映射提示表路径
- `dataSourceType`: 可选，`EXCEL` 或 `CSV`
- `topK`: 可选，默认 `5`
- `createdBy`: 可选
- `forceRebuild`: 可选，默认 `false`。开启后会强制重建提取、解析和匹配任务。

示例：

```bash
curl -X POST "http://localhost:8080/api/valuation-workflows/full-process" \
  -F "file=@./input/20230321基金资产估值表DJ0233.xls" \
  -F "dataSourceType=EXCEL" \
  -F "standardWorkbookPath=./standard/光大标准科目（解密后）.xlsx" \
  -F "mappingWorkbookPath=./standard/光大科目映射（解密后）.xlsx" \
  -F "topK=5" \
  -F "createdBy=workflow-user"
```

返回重点字段：

- `fileId`
- `extractTask`
- `parseTask`
- `matchTask`
- `fileFingerprint`
- `extractTask.reusedExistingTask`: 是否复用了已有抽取任务
- 三个任务对象都会带上 `taskStage`、`taskStartTime` 和对应阶段耗时字段

## 5. 查询 ODS 原始数据

`GET /api/valuation-workflows/{fileId}/raw-data?limit=200`

示例：

```bash
curl "http://localhost:8080/api/valuation-workflows/10001/raw-data?limit=200"
```

## 6. 查询 STG 解析快照

`GET /api/valuation-workflows/{fileId}/stg-data`

示例：

```bash
curl "http://localhost:8080/api/valuation-workflows/10001/stg-data"
```

返回结构包括：

- 标题信息
- 基础信息
- 表头
- 多层表头明细
- 列级表头元数据
- 原始解析明细
- 原始解析指标行

## 7. 查询 DWD 标准数据

`GET /api/valuation-workflows/{fileId}/dwd-data`

示例：

```bash
curl "http://localhost:8080/api/valuation-workflows/10001/dwd-data"
```

返回结构包括：

- 标题信息
- 基础信息
- 表头
- 多层表头明细
- 列级表头元数据
- 标准化后的外部估值数据明细
- 标准化后的指标行数据

## 8. 查询匹配结果

`GET /api/valuation-workflows/{fileId}/match-results`

示例：

```bash
curl "http://localhost:8080/api/valuation-workflows/10001/match-results"
```

## 推荐调用顺序

1. 调 `/upload` 拿到 `fileId`
2. 调 `/analyze` 生成 DWD 标准数据
3. 调 `/match` 生成匹配结果
4. 调 `/raw-data`、`/stg-data`、`/dwd-data`、`/match-results` 查询结果

如果不需要分步控制，直接调用 `/full-process`。

## 9. 查询单个任务

`GET /api/tasks/{taskId}`

说明：

- 可用于查看 `taskStage`、`taskStartTime`、`parseTaskTimeMs`、`standardizeTimeMs`、`matchStandardSubjectTimeMs`
- 对 `EXTRACT_DATA` 任务还会返回 `rowCount`、`fileSizeBytes`，并将文件解析耗时写入 `parseTaskTimeMs`
- 对 `PARSE_WORKBOOK` 任务会将结构标准化耗时写入 `standardizeTimeMs`
- 对 `MATCH_SUBJECT` 任务会将匹配标准科目耗时写入 `matchStandardSubjectTimeMs`
- 任务是否复用由 `extractTask.reusedExistingTask` 或 `upload` / `full-process` 返回值体现

## 10. 文件管理接口

### 9.1 手动上传文件

`POST /api/files/upload`

说明：

- 与 `/api/valuation-workflows/upload` 等价
- 返回 `fileId`、`fileFingerprint`、`fileStatus`

### 9.2 查询文件主数据

`GET /api/files/{fileId}`

### 9.3 按条件搜索文件主数据

`GET /api/files?sourceChannel=&fileStatus=&fingerprint=&limit=`

### 9.4 查询文件接入日志

`GET /api/files/{fileId}/ingest-logs`

### 9.5 查询 Excel sheet 样式快照

`GET /api/files/{fileId}/sheet-styles`

说明：

- 仅 Excel 文件会写入 sheet 样式快照
- 返回标题、header、合并区对应的 Univer 结构快照
- CSV 文件或关闭样式解析开关时，返回空列表

示例返回：

```json
[
  {
    "id": 101,
    "taskId": 202,
    "fileId": 303,
    "sheetName": "Sheet1",
    "styleScope": "HEADER_PREVIEW",
    "sheetStyleJson": "{\"title\":\"demo\",\"mergeAreas\":[{\"firstRow\":0,\"lastRow\":1,\"firstColumn\":0,\"lastColumn\":2}]}",
    "titleRows": [
      {
        "rowIndex": 0,
        "texts": ["估值表标题"]
      }
    ],
    "headerRows": [
      {
        "rowIndex": 1,
        "texts": ["科目代码", "科目名称"]
      }
    ],
    "mergeAreas": [
      {
        "firstRow": 0,
        "lastRow": 1,
        "firstColumn": 0,
        "lastColumn": 2
      }
    ],
    "previewRowCount": 20,
    "createdAt": "2026-04-15T12:30:00"
  }
]
```
