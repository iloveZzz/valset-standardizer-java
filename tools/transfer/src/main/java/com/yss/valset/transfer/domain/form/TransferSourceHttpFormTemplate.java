package com.yss.valset.transfer.domain.form;

import com.yss.valset.transfer.domain.form.model.Mode;
import com.yss.valset.transfer.domain.form.model.YssFormDefinition;
import com.yss.valset.transfer.domain.form.model.YssFormilyDsl;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP 来源表单模板。
 */
@Component
public class TransferSourceHttpFormTemplate extends FormTemplate {

    @Override
    public String getName() {
        return "transfer_source_http";
    }

    @Override
    public Map<String, Object> initialValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(TransferConfigKeys.ALLOW_MULTIPLE_FILES, Boolean.TRUE);
        values.put(TransferConfigKeys.LIMIT, 100);
        values.put("enabled", Boolean.FALSE);
        return values;
    }

    @Override
    public String getDescription() {
        return "HTTP 文件上传来源配置";
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
                        YssFormilyDsl.input("sourceCode", "来源编码").required().placeholder("例如：http-inbox").gridSpan(1),
                        YssFormilyDsl.input("sourceName", "来源名称").required().placeholder("例如：HTTP 文件收件箱").gridSpan(1),
                        YssFormilyDsl.switchField("enabled", "启用").gridSpan(1),
                        YssFormilyDsl.switchField(TransferConfigKeys.ALLOW_MULTIPLE_FILES, "允许多文件上传").gridSpan(1),
                        YssFormilyDsl.inputNumber(TransferConfigKeys.LIMIT, "收取上限").placeholder("100").gridSpan(1)
                )
                .build();
    }
}
