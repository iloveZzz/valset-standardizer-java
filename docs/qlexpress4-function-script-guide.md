# QLExpress4 函数脚本技术说明

本文说明本项目中 `T_QLEXPRESS_FUNCTION` 的函数脚本写法。语法参考 `/Users/zhudaoming/Downloads/README.adoc` 中的 QLExpress4 官方说明，但以当前工程实际依赖的 `qlexpress4` 4.0.4 运行结果为准。

## 运行边界

- 公开函数以 `T_QLEXPRESS_FUNCTION.SCRIPT_BODY` 为注册源，运行时通过 `QlexpressRunnerRegistry` 加载到对应 Runner。
- `sourceModules` 控制函数作用域：`common`、`extract.parse`、`extract.headerMapping`、`transfer.rule`。
- `extract.parse` 解析函数必须使用 QLExpress4 脚本自身实现，不再调用 `qlParseFns`、`ParseRuleSupport` 或解析 Java 门面。
- 项目继续使用默认隔离安全策略，不开启 `QLSecurityStrategy.open()`。
- 新增或启用函数前必须通过函数管理接口或单元测试验证脚本注册和实际表达式执行。

## 推荐语法

本项目已验证可稳定使用的写法：

```java
// List 下标和长度
row[0]
row.length

// for-each
for (cell : row) {
  // ...
};

// for 循环
for (i = 0; i < row.length; i++) {
  // ...
};

// JSON List / Map
keywords = ["科目代码", "科目名称"];
result = {"subjectCode": row[0], "subjectName": row[1]};

// 空 Map
m = {:};
m["value"] = "999";

// 字符串包含
"科目" in "科目代码"

// 简单 LIKE，按 SQL LIKE 风格使用 % 通配
"1001" like "1%"
```

动态抽取规则优先使用 JSON Map 字面量：

```java
{"subjectCode": "S-" + row[0], "subjectName": "动态科目"}
```

空结果优先使用：

```java
{:}
```

## 禁用写法

以下写法在 4.0.4 默认安全策略下不可用或高风险，不要写进数据库函数或规则表达式：

```java
row.get(0)
row.size()
text.contains("科目")
text.trim()
new java.util.HashMap()
String(value)
var value = 1
```

不要在脚本里直接访问 Java 类、字段或方法，也不要为了解决脚本问题打开 `QLSecurityStrategy.open()`。如果遇到 `METHOD_NOT_FOUND`、`FIELD_NOT_FOUND`、`FUNCTION_NOT_FOUND`，优先改成本文推荐语法，或把能力设计成明确的数据库函数脚本。

## 函数迁移规则

- 解析函数名可以继续保持兼容，例如 `rowContainsAll`、`isHeaderRow`、`classifyRowWithPattern`、`textAt`、`mapOf`。
- 保留兼容函数名不代表保留 Java 实现；这些函数必须由 `SCRIPT_BODY` 纯脚本实现。
- 解析业务判断应在脚本函数中组合完成，Java 侧只负责传入 `row`、`requiredHeaders`、`footerKeywords`、`subjectCodePattern` 等上下文。
- `mapOf/newMap/put` 仅作为兼容函数存在，新规则优先写 JSON Map 字面量。
- 函数间可以互相调用，但要确保被调用函数属于相同 Runner scope 或 `common` scope。

## transfer.rule 边界

`transfer.rule` 函数同样由 `SCRIPT_BODY` 纯脚本实现，不再调用 `qlTransferFns` 或 `TransferRuleFunctions`。脚本只能处理运行上下文中已经准备好的 String、List、Map。

Java 侧仍负责基础设施输入准备：

- `previewRows`：文件前 N 行预览数据，由 Java 在进入脚本前读取 Excel/CSV。
- `productMatchRules`：产品识别规则的 Map 列表，脚本通过 `rule["pdCd"]`、`rule["matchKeywords"]` 等字段访问。
- `tagMeta`、`ruleMeta`：标签或路由规则扩展配置。

因此，脚本中不要再写“读取文件”的业务流程。兼容函数 `readExcelDataWithin(source, maxRows)`、`readCsvDataWithin(source, maxRows)` 只返回传入的预览行参数，不触发文件系统访问。估值表判断应显式传入 `previewRows`：

```java
return isValuationTableByMeta(previewRows, tagMeta);
```

## 调试和验收

新增函数至少验证三件事：

1. `addFunctionsDefinedInScript` 能成功注册，且函数名与配置的 `FUNCTION_NAME` 完全一致。
2. 用真实上下文执行目标表达式，确认结果类型符合调用方预期，例如布尔、字符串或 Map。
3. 脚本体不包含已移除桥接或禁用语法：`qlParseFns`、`qlTransferFns`、`ParseRuleSupport`、`TransferRuleFunctions`、`row.get(`、`.size()`、`.contains(`。

推荐用例：

```java
rowContainsAll(row, requiredHeaders)
isHeaderRow(row, requiredHeaders)
classifyRowWithPattern(row, footerKeywords, subjectCodePattern)
{"subjectCode": row[0], "rawValues": {"value": row[2]}}
```
