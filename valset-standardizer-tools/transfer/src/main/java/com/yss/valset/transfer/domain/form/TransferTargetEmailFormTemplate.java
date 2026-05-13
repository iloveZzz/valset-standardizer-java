package com.yss.valset.transfer.domain.form;

import com.yss.valset.transfer.domain.form.model.Mode;
import com.yss.valset.transfer.domain.form.model.YssFormDefinition;
import com.yss.valset.transfer.domain.form.model.YssFormilyDsl;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 邮件目标表单模板。
 */
@Component
public class TransferTargetEmailFormTemplate extends FormTemplate {

    @Override
    public String getName() {
        return "transfer_target_email";
    }

    @Override
    public Map<String, Object> initialValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(TransferConfigKeys.PROTOCOL, "smtp");
        values.put(TransferConfigKeys.PORT, 25);
        values.put(TransferConfigKeys.AUTH, Boolean.FALSE);
        values.put(TransferConfigKeys.START_TLS, Boolean.FALSE);
        values.put(TransferConfigKeys.SSL, Boolean.FALSE);
        values.put(TransferConfigKeys.TIMEOUT_MILLIS, 10000);
        values.put(TransferConfigKeys.FORWARD_MAIL_CONTENT, Boolean.FALSE);
        values.put(TransferConfigKeys.FORWARD_ORIGINAL_SENDER, Boolean.FALSE);
        values.put("enabled", Boolean.FALSE);
        return values;
    }

    @Override
    public String getDescription() {
        return "邮件目标配置";
    }

    @Override
    public String getCategory() {
        return "transfer_target";
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
                        YssFormilyDsl.input("targetCode", "目标编码").required().placeholder("例如：mail-forward").gridSpan(1),
                        YssFormilyDsl.input("targetName", "目标名称").required().placeholder("例如：邮件转发").gridSpan(1),
                        YssFormilyDsl.switchField("enabled", "启用").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.HOST, "SMTP 主机").required().placeholder("smtp.example.com").gridSpan(1),
                        YssFormilyDsl.inputNumber(TransferConfigKeys.PORT, "SMTP 端口").placeholder("25").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.USERNAME, "用户名").placeholder("可选").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.PASSWORD, "密码").componentProp("type", "password").placeholder("可选").gridSpan(1),
                        YssFormilyDsl.select(TransferConfigKeys.PROTOCOL, "协议")
                                .required()
                                .options(
                                        YssFormilyDsl.option("smtp", "SMTP"),
                                        YssFormilyDsl.option("smtps", "SMTPS")
                                )
                                .gridSpan(1),
                        YssFormilyDsl.switchField(TransferConfigKeys.AUTH, "需要认证").gridSpan(1),
                        YssFormilyDsl.switchField(TransferConfigKeys.START_TLS, "STARTTLS").gridSpan(1),
                        YssFormilyDsl.switchField(TransferConfigKeys.SSL, "SSL").gridSpan(1),
                        YssFormilyDsl.inputNumber(TransferConfigKeys.TIMEOUT_MILLIS, "超时(毫秒)").placeholder("10000").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.TO, "收件人").required().placeholder("to@example.com，多个收件人使用“,”号分割").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.CC, "抄送").placeholder("可选：to@example.com，多人使用“,”号分割").gridSpan(1),
                        YssFormilyDsl.textArea(TransferConfigKeys.SUBJECT_TEMPLATE, "主题模板").placeholder("可选，支持 ${fileName} 等变量").gridSpan(2),
                        YssFormilyDsl.textArea(TransferConfigKeys.BODY_TEMPLATE, "正文模板").placeholder("可选，支持 ${mailSubject} 等变量").gridSpan(2),
                        YssFormilyDsl.switchField(TransferConfigKeys.FORWARD_MAIL_CONTENT, "转发原始邮件内容").gridSpan(1),
                        YssFormilyDsl.switchField(TransferConfigKeys.FORWARD_ORIGINAL_SENDER, "保留原始发件人").gridSpan(1)
                )
                .build();
    }
}
