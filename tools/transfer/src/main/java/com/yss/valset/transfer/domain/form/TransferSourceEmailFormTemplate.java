package com.yss.valset.transfer.domain.form;

import com.yss.valset.transfer.domain.form.model.Mode;
import com.yss.valset.transfer.domain.form.model.YssFormDefinition;
import com.yss.valset.transfer.domain.form.model.YssFormilyDsl;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 邮件来源表单模板。
 */
@Component
public class TransferSourceEmailFormTemplate extends FormTemplate {

    @Override
    public String getName() {
        return "transfer_source_email";
    }

    @Override
    public Map<String, Object> initialValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(TransferConfigKeys.PROTOCOL, "imap");
        values.put(TransferConfigKeys.FOLDER, "INBOX");
        values.put(TransferConfigKeys.MAIL_TIME_RANGE_DAYS, "0");
        values.put(TransferConfigKeys.SSL, Boolean.FALSE);
        values.put(TransferConfigKeys.START_TLS, Boolean.FALSE);
        values.put(TransferConfigKeys.LIMIT, 100);
        values.put("enabled", Boolean.FALSE);
        values.put("pollCron", "0 */5 * * * ?");
        return values;
    }

    @Override
    public String getDescription() {
        return "邮件来源配置";
    }

    @Override
    public String getCategory() {
        return "transfer_source";
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
                        YssFormilyDsl.input("sourceCode", "来源编码").required().placeholder("例如：mail-inbox").gridSpan(1),
                        YssFormilyDsl.input("sourceName", "来源名称").required().placeholder("例如：邮件收件箱").gridSpan(1),
                        YssFormilyDsl.switchField("enabled", "启用").gridSpan(1),
                        YssFormilyDsl.slot("pollCron", "轮询表达式","pollCron").placeholder("0 */5 * * * ?").gridSpan(1),
                        YssFormilyDsl.select(TransferConfigKeys.PROTOCOL, "协议")
                                .required()
                                .options(
                                        YssFormilyDsl.option("imap", "IMAP"),
                                        YssFormilyDsl.option("imaps", "IMAPS"),
                                        YssFormilyDsl.option("pop3", "POP3"),
                                        YssFormilyDsl.option("pop3s", "POP3S")
                                )
                                .gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.HOST, "主机地址").required().placeholder("mail.example.com").gridSpan(1),
                        YssFormilyDsl.inputNumber(TransferConfigKeys.PORT, "端口").required().placeholder("993").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.USERNAME, "用户名").required().placeholder("mail-user").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.PASSWORD, "密码").required().componentProp("type", "password").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.FOLDER, "文件夹").required().placeholder("INBOX").gridSpan(1),
                        YssFormilyDsl.select(TransferConfigKeys.MAIL_TIME_RANGE_DAYS, "收取时间范围")
                                .required()
                                .options(
                                        YssFormilyDsl.option("0", "全部邮件"),
                                        YssFormilyDsl.option("7", "近7天"),
                                        YssFormilyDsl.option("15", "近15天"),
                                        YssFormilyDsl.option("30", "近30天"),
                                        YssFormilyDsl.option("90", "近90天")
                                )
                                .gridSpan(1),
                        YssFormilyDsl.switchField(TransferConfigKeys.SSL, "SSL").gridSpan(1),
                        YssFormilyDsl.switchField(TransferConfigKeys.START_TLS, "STARTTLS").gridSpan(1),
                        YssFormilyDsl.groupHeader("header2", "收取规则配置"),
//                        YssFormilyDsl.inputNumber(TransferConfigKeys.LIMIT, "收取上限").placeholder("100").gridSpan(1),
                        YssFormilyDsl.slot("mailCondition", "邮箱条件","mailCondition").placeholder("邮箱收取条件配置").gridSpan(2)
                )
                .build();
    }
}
