package com.yss.valset.transfer.domain.form;

import com.yss.valset.transfer.domain.form.model.Mode;
import com.yss.valset.transfer.domain.form.model.YssFormDefinition;
import com.yss.valset.transfer.domain.form.model.YssFormilyDsl;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SFTP 目标表单模板。
 */
@Component
public class TransferTargetSftpFormTemplate extends FormTemplate {

    @Override
    public String getName() {
        return "transfer_target_sftp";
    }

    @Override
    public Map<String, Object> initialValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(TransferConfigKeys.PORT, 22);
        values.put(TransferConfigKeys.STRICT_HOST_KEY_CHECKING, Boolean.FALSE);
        values.put(TransferConfigKeys.CONNECT_TIMEOUT_MILLIS, 10000);
        values.put(TransferConfigKeys.CHANNEL_TIMEOUT_MILLIS, 10000);
        values.put("enabled", Boolean.FALSE);
        return values;
    }

    @Override
    public String getDescription() {
        return "SFTP 目标配置";
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
                        YssFormilyDsl.input("targetCode", "目标编码").required().placeholder("例如：sftp-archive").gridSpan(1),
                        YssFormilyDsl.input("targetName", "目标名称").required().placeholder("例如：SFTP 归档").gridSpan(1),
                        YssFormilyDsl.switchField("enabled", "启用").gridSpan(1),
                        YssFormilyDsl.input("targetPathTemplate", "目标路径模板").placeholder("例如：archive/").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.HOST, "主机地址").required().placeholder("sftp.example.com").gridSpan(1),
                        YssFormilyDsl.inputNumber(TransferConfigKeys.PORT, "端口").placeholder("22").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.USERNAME, "用户名").required().placeholder("sftp-user").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.PASSWORD, "密码").componentProp("type", "password").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.PRIVATE_KEY_PATH, "私钥路径").placeholder("/home/user/.ssh/id_rsa").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.PASSPHRASE, "私钥口令").componentProp("type", "password").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.REMOTE_DIR, "远程目录").required().placeholder("/archive").gridSpan(2),
                        YssFormilyDsl.switchField(TransferConfigKeys.STRICT_HOST_KEY_CHECKING, "严格校验主机密钥").gridSpan(1),
                        YssFormilyDsl.inputNumber(TransferConfigKeys.CONNECT_TIMEOUT_MILLIS, "连接超时(毫秒)").placeholder("10000").gridSpan(1),
                        YssFormilyDsl.inputNumber(TransferConfigKeys.CHANNEL_TIMEOUT_MILLIS, "通道超时(毫秒)").placeholder("10000").gridSpan(1)
                )
                .build();
    }
}
