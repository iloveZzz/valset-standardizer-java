package com.yss.valset.transfer.domain.form;

import com.yss.valset.transfer.domain.form.model.Mode;
import com.yss.valset.transfer.domain.form.model.YssFormDefinition;
import com.yss.valset.transfer.domain.form.model.YssFormilyDsl;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.domain.model.config.TransferRouteGroupStrategy;
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
        values.put("groupField", "group");
        values.put(TransferConfigKeys.TARGET_TYPE, TargetType.FILESYS.name());
        values.put(TransferConfigKeys.TARGET_CODE, "filesys-archive");
        values.put(TransferConfigKeys.TARGET_PATH, "/transfer/inbox");
        values.put(TransferConfigKeys.RENAME_PATTERN, "${fileName}");
        values.put(TransferConfigKeys.RETRY_DELAY_SECONDS, 60);
        values.put(TransferConfigKeys.GROUP_STRATEGY, TransferRouteGroupStrategy.NONE.name());
        values.put(TransferConfigKeys.GROUP_TARGET_MAPPING, "{\n  \"finance@example.com\": \"filesys-finance\",\n  \"ops@example.com\": \"filesys-ops\"\n}");
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
                .gridDefaults(2, 1, 260, 16, 0)
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
                        YssFormilyDsl.input(TransferConfigKeys.RENAME_PATTERN, "重命名模板").placeholder("${fileName}").gridSpan(1),
                        YssFormilyDsl.groupHeader("group", "分组路由配置"),
                        YssFormilyDsl.select(TransferConfigKeys.GROUP_STRATEGY, "分组策略")
                                .required()
                                .options(
                                        YssFormilyDsl.option(TransferRouteGroupStrategy.NONE.name(), "不分组"),
                                        YssFormilyDsl.option(TransferRouteGroupStrategy.FILE_TYPE.name(), "按文件类型"),
                                        YssFormilyDsl.option(TransferRouteGroupStrategy.FILE_NAME.name(), "按文件名称"),
                                        YssFormilyDsl.option(TransferRouteGroupStrategy.MAIL_FROM.name(), "按邮件发件人"),
                                        YssFormilyDsl.option(TransferRouteGroupStrategy.MAIL_TO.name(), "按邮件收件人"),
                                        YssFormilyDsl.option(TransferRouteGroupStrategy.REG_RULE.name(), "按正则分组"),
                                        YssFormilyDsl.option(TransferRouteGroupStrategy.CUSTOM.name(), "自定义字段")
                                )
                                .gridSpan(1),
                        YssFormilyDsl.input("regGroup", "正则分组")
                                .visibleExpr("{{ $values.groupStrategy === 'REG_RULE' }}")
                                .required()
                                .placeholder("请输入正则表达式").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.GROUP_FIELD, "分组字段")
                                .placeholder("自定义分组时使用")
                                .required()
                                .gridSpan(1),
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
                        YssFormilyDsl.groupHeader("script", "匹配规则脚本"),
                        YssFormilyDsl.textArea("scriptBody", "脚本内容")
                                .required()
                                .visibleExpr("{{ $values.matchStrategy === 'SCRIPT_RULE' }}")
                                .placeholder("返回 true/false 或规则结果对象")
                                .gridSpan(16)
                )
                .build();
    }
}
