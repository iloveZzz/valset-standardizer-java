package com.yss.valset.transfer.domain.form;

import com.yss.valset.transfer.domain.form.model.Mode;
import com.yss.valset.transfer.domain.form.model.YssFormDefinition;
import com.yss.valset.transfer.domain.form.model.YssFormilyDsl;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * yss-filesys 目标表单模板。
 */
@Component
public class TransferTargetFilesysFormTemplate extends FormTemplate {

    @Override
    public String getName() {
        return "transfer_target_filesys";
    }

    @Override
    public Map<String, Object> initialValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("targetPathTemplate", "");
        values.put(TransferConfigKeys.CHUNK_SIZE, 8L * 1024 * 1024);
        values.put("enabled", Boolean.FALSE);
        return values;
    }

    @Override
    public String getDescription() {
        return "yss-filesys 目标配置";
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
                        YssFormilyDsl.input("targetCode", "目标编码").required().placeholder("例如：filesys-archive").gridSpan(1),
                        YssFormilyDsl.input("targetName", "目标名称").required().placeholder("例如：文件服务归档").gridSpan(1),
                        YssFormilyDsl.switchField("enabled", "启用").gridSpan(1),
                        YssFormilyDsl.input("targetPathTemplate", "目标路径模板").required().placeholder("例如：/archive/${yyyyMMdd}/").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.PARENT_ID, "父目录标识").required().placeholder("请输入父目录标识").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.STORAGE_SETTING_ID, "存储配置标识").required().placeholder("请输入存储配置标识").gridSpan(1),
                        YssFormilyDsl.inputNumber(TransferConfigKeys.CHUNK_SIZE, "分片大小(字节)").placeholder("8388608").gridSpan(1)
                )
                .build();
    }
}
