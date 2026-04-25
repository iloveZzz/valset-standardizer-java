package com.yss.valset.transfer.domain.form;

import com.yss.valset.transfer.domain.form.model.Mode;
import com.yss.valset.transfer.domain.form.model.YssFormDefinition;
import com.yss.valset.transfer.domain.form.model.YssFormilyDsl;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 标签管理表单模板。
 */
@Component
public class TransferTagFormTemplate extends FormTemplate {

    private static final String DEFAULT_VALUATION_TABLE_SCRIPT = """
            String source = hasText(filePath) ? filePath : path;
            if (!hasText(source)) {
                return false;
            }
            if (!(isExcelFile(source) || isCsvFile(source))) {
                return false;
            }
            return isValuationTableByMeta(source, tagMeta);
            """;

    @Override
    public String getName() {
        return TransferFormTemplateNames.TRANSFER_TAG;
    }

    @Override
    public Map<String, Object> initialValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", Boolean.TRUE);
        values.put("priority", 10);
        values.put("matchStrategy", "SCRIPT_RULE");
        values.put("scriptLanguage", "qlexpress4");
        values.put("scriptBody", DEFAULT_VALUATION_TABLE_SCRIPT);
        Map<String, Object> tagMeta = new LinkedHashMap<>();
        tagMeta.put("scanLimit", 100);
        tagMeta.put("headerKeywords", java.util.List.of("科目代码", "科目名称"));
        values.put("tagMeta", tagMeta);
        return values;
    }

    @Override
    public String getDescription() {
        return "标签管理配置";
    }

    @Override
    public String getCategory() {
        return "transfer_tag";
    }

    @Override
    public YssFormDefinition buildForm() {
        return YssFormilyDsl.form()
                .mode(Mode.EDIT)
                .horizontal(120)
                .gridDefaults(2, 1, 320, 14, 0)
                .initialValues(initialValues())
                .detailOption("bordered", true)
                .detailOption("maxColumns", 2)
                .nodes(
                        YssFormilyDsl.input("tagCode", "标签编码").required().placeholder("例如：EMAIL_ATTACHMENT_TYPE").gridSpan(1),
                        YssFormilyDsl.input("tagName", "标签名称").required().placeholder("例如：邮件附件类型").gridSpan(1),
                        YssFormilyDsl.input("tagValue", "标签值").required().placeholder("例如：xlsx").gridSpan(1),
                        YssFormilyDsl.switchField("enabled", "启用").gridSpan(1),
                        YssFormilyDsl.select("matchStrategy", "匹配策略")
                                .required()
                                .options(
                                        YssFormilyDsl.option("SCRIPT_RULE", "脚本规则"),
                                        YssFormilyDsl.option("REGEX_RULE", "正则规则")
                                )
                                .gridSpan(1),

                        YssFormilyDsl.slot("regexPattern", "正则表达式", "regexPattern")
                                .required()
                                .visibleExpr("{{ $values.matchStrategy === 'REGEX_RULE' || $values.matchStrategy === 'SCRIPT_AND_REGEX' || $values.matchStrategy === 'SCRIPT_OR_REGEX' }}")
                                .placeholder("例如：^.*\\.(xlsx|xls)$")
                                .gridSpan(1),
                        YssFormilyDsl.select("scriptLanguage", "脚本语言")
                                .required()
                                .visibleExpr("{{ $values.matchStrategy === 'SCRIPT_RULE' || $values.matchStrategy === 'SCRIPT_AND_REGEX' || $values.matchStrategy === 'SCRIPT_OR_REGEX' }}")
                                .options(
                                        YssFormilyDsl.option("qlexpress4", "QLExpress4")
                                )
                                .gridSpan(1),
                        YssFormilyDsl.slot("scriptBody", "脚本内容", "scriptBody")
                                .visibleExpr("{{ $values.matchStrategy === 'SCRIPT_RULE' || $values.matchStrategy === 'SCRIPT_AND_REGEX' || $values.matchStrategy === 'SCRIPT_OR_REGEX' }}")
                                .placeholder("返回 true/false，或返回包含命中信息的对象")
                                .gridSpan(2),
                        YssFormilyDsl.slot("tagMeta", "扩展配置", "tagMeta")
                                .visibleExpr("{{ $values.matchStrategy === 'SCRIPT_RULE' || $values.matchStrategy === 'SCRIPT_AND_REGEX' || $values.matchStrategy === 'SCRIPT_OR_REGEX' }}")
                                .placeholder("{\n  \"scanLimit\": 100,\n  \"headerKeywords\": [\"科目代码\", \"科目名称\"]\n}")
                                .gridSpan(2)
                )
                .build();
    }
}
