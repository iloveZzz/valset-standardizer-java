package com.yss.valset.transfer.domain.form;

import com.yss.valset.transfer.domain.form.model.Mode;
import com.yss.valset.transfer.domain.form.model.YssFormDefinition;
import com.yss.valset.transfer.domain.form.model.YssFormilyDsl;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.domain.model.TargetType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 路由规则表单模板。
 */
@Component
public class TransferRuleFormTemplate extends FormTemplate {

    @Override
    public String getName() {
        return TransferFormTemplateNames.TRANSFER_RULE;
    }

    @Override
    public Map<String, Object> initialValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", Boolean.FALSE);
        values.put("priority", 10);
        values.put("ruleVersion", "1.0.0");
        values.put("matchStrategy", "ALL");
        values.put("scriptLanguage", "qlexpress4");
        values.put(TransferConfigKeys.TARGET_TYPE, TargetType.FILESYS.name());
        values.put(TransferConfigKeys.TARGET_CODE, "filesys-archive");
        values.put(TransferConfigKeys.TARGET_PATH, "/transfer/inbox");
        values.put(TransferConfigKeys.RETRY_DELAY_SECONDS, 60);
        return values;
    }

    @Override
    public String getDescription() {
        return "路由规则配置";
    }

    @Override
    public String getCategory() {
        return "transfer_rule";
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
                        YssFormilyDsl.input("ruleCode", "规则编码").required().placeholder("例如：EMAIL_DAILY_REPORT_TO_FILESYS").gridSpan(1),
                        YssFormilyDsl.input("ruleName", "规则名称").required().placeholder("例如：邮件日报附件转存文件服务").gridSpan(1),
                        YssFormilyDsl.input("ruleVersion", "规则版本").placeholder("1.0.0").gridSpan(1),
                        YssFormilyDsl.switchField("enabled", "启用").gridSpan(1),
                        YssFormilyDsl.inputNumber("priority", "优先级").placeholder("10").gridSpan(1),
                        YssFormilyDsl.inputNumber(TransferConfigKeys.RETRY_DELAY_SECONDS, "重试间隔(秒)").placeholder("60").gridSpan(1),
                        YssFormilyDsl.select("matchStrategy", "匹配策略")
                                .required()
                                .options(
                                        YssFormilyDsl.option("ALL", "默认全部"),
                                        YssFormilyDsl.option("SCRIPT_RULE", "脚本规则")
                                )
                                .gridSpan(1),
                        YssFormilyDsl.select("scriptLanguage", "脚本语言")
                                .required()
                                .visibleExpr("{{ $values.matchStrategy === 'SCRIPT_RULE' }}")
                                .options(
                                        YssFormilyDsl.option("qlexpress4", "QLExpress4"),
                                        YssFormilyDsl.option("Javascript", "Javascript")
                                )
                                .gridSpan(1),
                        YssFormilyDsl.slot("scriptBody", "脚本内容","scriptBody")
                                .required()
                                .visibleExpr("{{ $values.matchStrategy === 'SCRIPT_RULE' }}")
                                .placeholder("返回 true/false 或规则结果对象")
                                .gridSpan(2)
                )
                .build();
    }
}
