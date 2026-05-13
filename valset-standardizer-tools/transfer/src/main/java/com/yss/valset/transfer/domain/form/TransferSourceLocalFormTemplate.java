package com.yss.valset.transfer.domain.form;

import com.yss.valset.transfer.domain.form.model.Mode;
import com.yss.valset.transfer.domain.form.model.YssFormDefinition;
import com.yss.valset.transfer.domain.form.model.YssFormilyDsl;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 本地目录来源表单模板。
 */
@Component
public class TransferSourceLocalFormTemplate extends FormTemplate {

    @Override
    public String getName() {
        return "transfer_source_local";
    }

    @Override
    public Map<String, Object> initialValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(TransferConfigKeys.DIRECTORY, "/tmp/yss-transfer/inbox");
        values.put(TransferConfigKeys.RECURSIVE, Boolean.FALSE);
        values.put(TransferConfigKeys.INCLUDE_HIDDEN, Boolean.FALSE);
        values.put(TransferConfigKeys.LIMIT, 100);
        values.put("enabled", Boolean.FALSE);
        return values;
    }

    @Override
    public String getDescription() {
        return "本地目录来源配置";
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
                        YssFormilyDsl.input("sourceCode", "来源编码").required().placeholder("例如：local-inbox").gridSpan(1),
                        YssFormilyDsl.input("sourceName", "来源名称").required().placeholder("例如：本地收件箱").gridSpan(1),
                        YssFormilyDsl.switchField("enabled", "启用").gridSpan(1),
                        YssFormilyDsl.switchField(TransferConfigKeys.RECURSIVE, "递归扫描").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.DIRECTORY, "目录路径").required().placeholder("/tmp/yss-transfer/inbox").gridSpan(2),
                        YssFormilyDsl.switchField(TransferConfigKeys.INCLUDE_HIDDEN, "包含隐藏文件").gridSpan(1),
                        YssFormilyDsl.inputNumber(TransferConfigKeys.LIMIT, "扫描上限").placeholder("100").gridSpan(1)
                )
                .build();
    }
}
