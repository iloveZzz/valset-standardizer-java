package com.yss.valset.transfer.domain.form;

import com.yss.valset.transfer.domain.form.model.Mode;
import com.yss.valset.transfer.domain.form.model.YssFormDefinition;
import com.yss.valset.transfer.domain.form.model.YssFormilyDsl;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务日期提取标签模板。
 */
@Component
public class TransferTagBusinessDateFormTemplate extends FormTemplate {

    private static final String BUSINESS_DATE_REGEX_PATTERN = "(?:\\d{4}[-/]\\d{2}[-/]\\d{2}|\\d{8}|\\d{4}年\\d{1,2}月\\d{1,2}日)";

    @Override
    public String getName() {
        return TransferFormTemplateNames.TRANSFER_TAG_BUSINESS_DATE;
    }

    @Override
    public Map<String, Object> initialValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", Boolean.TRUE);
        values.put("priority", 10);
        values.put("matchStrategy", "REGEX_RULE");
        values.put("tagCode", "BUSINESS_DATE");
        values.put("tagName", "业务日期");
        values.put("tagValue", "DATE");
        values.put("regexPattern", BUSINESS_DATE_REGEX_PATTERN);
        Map<String, Object> tagMeta = new LinkedHashMap<>();
        tagMeta.put("candidateFields", List.of("fileName", "originalName", "subject", "path"));
        tagMeta.put("supportedFormats", List.of("yyyy-MM-dd", "yyyyMMdd", "yyyy/MM/dd", "yyyy年MM月dd日"));
        values.put("tagMeta", tagMeta);
        return values;
    }

    @Override
    public String getDescription() {
        return "业务日期提取规则配置";
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
                        YssFormilyDsl.groupHeader("businessDateGuide", "业务日期提取模板", "基于文件名、附件名、邮件主题或路径中的日期进行打标，命中后回填到分拣对象业务日期。")
                                .gridSpan(2),
                        YssFormilyDsl.input("tagCode", "标签编码").required().placeholder("例如：BUSINESS_DATE").gridSpan(1),
                        YssFormilyDsl.input("tagName", "标签名称").required().placeholder("例如：业务日期").gridSpan(1),
                        YssFormilyDsl.input("tagValue", "标签值").required().placeholder("例如：DATE").gridSpan(1),
                        YssFormilyDsl.switchField("enabled", "启用").gridSpan(1),
                        YssFormilyDsl.slot("regexPattern", "正则表达式", "regexPattern")
                                .required()
                                .placeholder("例如：(?:\\d{4}[-/]\\d{2}[-/]\\d{2}|\\d{8}|\\d{4}年\\d{1,2}月\\d{1,2}日)")
                                .gridSpan(2),
                        YssFormilyDsl.slot("tagMeta", "扩展配置", "tagMeta")
                                .placeholder("{\n  \"candidateFields\": [\"fileName\", \"originalName\", \"subject\", \"path\"],\n  \"supportedFormats\": [\"yyyy-MM-dd\", \"yyyyMMdd\", \"yyyy/MM/dd\", \"yyyy年MM月dd日\"]\n}")
                                .gridSpan(2)
                )
                .build();
    }
}
