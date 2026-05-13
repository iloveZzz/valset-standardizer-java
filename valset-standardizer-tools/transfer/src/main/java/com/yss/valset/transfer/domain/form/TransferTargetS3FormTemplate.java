package com.yss.valset.transfer.domain.form;

import com.yss.valset.transfer.domain.form.model.Mode;
import com.yss.valset.transfer.domain.form.model.YssFormDefinition;
import com.yss.valset.transfer.domain.form.model.YssFormilyDsl;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * S3 目标表单模板。
 */
@Component
public class TransferTargetS3FormTemplate extends FormTemplate {

    @Override
    public String getName() {
        return "transfer_target_s3";
    }

    @Override
    public Map<String, Object> initialValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(TransferConfigKeys.REGION, "cn-north-1");
        values.put(TransferConfigKeys.USE_PATH_STYLE, Boolean.FALSE);
        values.put("enabled", Boolean.FALSE);
        return values;
    }

    @Override
    public String getDescription() {
        return "S3 目标配置";
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
                        YssFormilyDsl.input("targetCode", "目标编码").required().placeholder("例如：s3-archive").gridSpan(1),
                        YssFormilyDsl.input("targetName", "目标名称").required().placeholder("例如：S3 归档").gridSpan(1),
                        YssFormilyDsl.switchField("enabled", "启用").gridSpan(1),
                        YssFormilyDsl.input("targetPathTemplate", "目标路径模板").placeholder("例如：/archive/${yyyyMMdd}/").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.BUCKET, "Bucket").required().placeholder("例如：my-bucket").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.REGION, "Region").required().placeholder("cn-north-1").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.ENDPOINT_URL, "Endpoint").placeholder("可选").gridSpan(2),
                        YssFormilyDsl.input(TransferConfigKeys.ACCESS_KEY, "Access Key").placeholder("可选").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.SECRET_KEY, "Secret Key").componentProp("type", "password").placeholder("可选").gridSpan(1),
                        YssFormilyDsl.switchField(TransferConfigKeys.USE_PATH_STYLE, "使用路径风格").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.KEY_PREFIX, "对象前缀").placeholder("例如：archive/").gridSpan(1)
                )
                .build();
    }
}
