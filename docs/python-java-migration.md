# Python 到 Java 迁移分析

## 1. Python 原型功能清单

当前 Python 项目可以分为 5 组核心能力：

1. 估值表解析
    - `parser.py`
    - `rules.py`
    - `subject_tree.py`
    - `summary.py`
2. 标准知识加载
    - `standard_subjects.py`
    - `mapping_dataset.py`
3. 匹配引擎
    - `subject_normalizer.py`
    - `anchor_selector.py`
    - `matcher.py`
    - `match_service.py`
4. 结果输出
    - `exporters.py`
    - `service.py`
5. 离线评估与调权
    - `mapping_evaluator.py`
    - `evaluate_mapping.py`

## 2. Java 当前迁移状态

### 2.1 已完成

- `PoiWorkbookParser`
    - 已支持 `xls/xlsx` 解析
    - 已支持主表头定位、标题/基础信息提取、科目/指标拆分
    - 已支持 `parentCode/rootCode/pathCodes/leaf` 计算
- `PoiStandardSubjectLoader`
    - 已支持标准科目表加载
    - 已支持路径、归一化文本、占位科目标记构建
- `PoiMappingHintLoader`
    - 已支持历史映射样本读取
    - 已支持名称/编码双索引构建
- `SimpleSubjectMatcher`
    - 已支持锚点选择
    - 已支持 root/keyword/token/history 候选召回
    - 已支持名称/路径/关键词/编码/历史分数组合评分
    - 已支持层级覆写规则和置信度分层
- `FileSystemResultExporter`
    - 已支持 `parsed_workbook.json`
    - 已支持 `subjects.csv / subject_relations.csv / metrics.csv`
    - 已支持 `subject_tree.json / summary.json / parsed.json`
    - 已支持 `parsed.duckdb`
    - 已支持 `match_results.json / match_top1.csv / match_candidates.csv / match_summary.json`
    - 已支持 `review_queue.csv / match.duckdb`
- `PoiMappingSampleLoader`
    - 已支持历史映射样本原始记录加载
    - 已与 `PoiMappingHintLoader` 复用同一份样本解析逻辑
- `EvaluateMappingExecutionAppServiceImpl`
    - 已支持 `org_holdout / hash_holdout` 切分
    - 已支持去重、调权子集构建、默认权重基线评估
    - 已支持权重搜索、失败样本聚类、评估结果导出
    - 已支持 `evaluation.duckdb` 导出
- embedding
    - 当前 Java 项目已明确暂停迁移 embedding 能力
    - 匹配与评估链路保持纯规则实现，embedding 权重不再开放
- 任务框架
    - 已支持 `EVALUATE_MAPPING` command / executor / controller 入口
    - 已支持 `mapping_evaluation.json / failure_cluster.json / weight_search_report.json / evaluation.duckdb`

### 2.2 待完成

- embedding 增强
    - 当前不作为迁移范围，后续如需恢复再单独设计
- 任务结果持久化细节
    - 已补齐 parse / match / evaluate 的结构化 `resultPayload`
    - 当前查询任务时可直接看到输出目录、关键指标、产物清单，以及结构化 `inputData/resultData`

## 3. 推荐的后续迁移批次

### 批次 A

- 已完成
- 解析类输出已与 Python 一期版本基本对齐

### 批次 B

- 已完成
- 匹配输出已补齐 `review_queue.csv`
- 匹配结果已补齐 `match.duckdb`

### 批次 C

- 已完成
- 已支持历史样本评估、权重建议、失败模式摘要
- 已接入 `EVALUATE_MAPPING` 任务执行链

## 4. 本次迁移结论

本次已经把 Python 原型的一期主链路迁到了 Java：

- 解析链路：可用
- 标准知识加载：可用
- 历史映射加载：可用
- 规则匹配链路：可用
- 结果输出闭环：可用
- 离线评估闭环：可用

当前 `subject-match-java` 已经从“纯骨架”升级为“可运行的解析 + 匹配 + 评估骨架”，后续重点转向规则链路完善、评估数据仓沉淀和任务完成态治理。
