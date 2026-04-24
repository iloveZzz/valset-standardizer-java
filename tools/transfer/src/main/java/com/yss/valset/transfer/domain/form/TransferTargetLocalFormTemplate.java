package com.yss.valset.transfer.domain.form;

import com.yss.valset.transfer.domain.form.model.Mode;
import com.yss.valset.transfer.domain.form.model.YssFormDefinition;
import com.yss.valset.transfer.domain.form.model.YssFormilyDsl;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 本地目录目标表单模板。
 */
@Component
public class TransferTargetLocalFormTemplate extends FormTemplate {

    @Override
    public String getName() {
        return "transfer_target_local";
    }

    @Override
    public Map<String, Object> initialValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("targetPathTemplate", "/tmp/yss-transfer/outbox");
        values.put("createParentDirectories", Boolean.TRUE);
        values.put("enabled", Boolean.FALSE);
        return values;
    }

    @Override
    public String getDescription() {
        return "本地目录目标配置";
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
                        YssFormilyDsl.input("targetCode", "目标编码").required().placeholder("例如：local-outbox").gridSpan(1),
                        YssFormilyDsl.input("targetName", "目标名称").required().placeholder("例如：本地目录归档").gridSpan(1),
                        YssFormilyDsl.switchField("enabled", "启用").gridSpan(1),
                        YssFormilyDsl.input("targetPathTemplate", "目标路径模板").placeholder("可选，支持 ${yyyyMMdd} 等变量").gridSpan(1),
                        YssFormilyDsl.switchField("createParentDirectories", "自动创建父目录").gridSpan(1)
                )
                .build();
    }
}
