package com.yss.valset.transfer.domain.form;

import com.yss.valset.transfer.domain.form.model.Mode;
import com.yss.valset.transfer.domain.form.model.YssFormDefinition;
import com.yss.valset.transfer.domain.form.model.YssFormilyDsl;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.domain.model.config.TransferRouteGroupStrategy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 路由配置表单模板。
 */
@Component
public class TransferRouteFormTemplate extends FormTemplate {

    @Override
    public String getName() {
        return TransferFormTemplateNames.TRANSFER_ROUTE;
    }

    @Override
    public Map<String, Object> initialValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("routeStatus", TransferStatus.PENDING.name());
        values.put("ruleId", 0L);
        values.put("sourceType", SourceType.LOCAL_DIR.name());
        values.put(TransferConfigKeys.SOURCE_CODE, "source-default");
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
        return "来源到目标的路由配置";
    }

    @Override
    public String getCategory() {
        return "transfer_route";
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
                        YssFormilyDsl.groupHeader("header1", "来源信息"),
                        YssFormilyDsl.inputNumber("sourceId", "来源主键").placeholder("可选").gridSpan(1),
                        YssFormilyDsl.select("sourceType", "来源类型")
                                .required()
                                .options(
                                        YssFormilyDsl.option(SourceType.LOCAL_DIR.name(), "本地目录"),
                                        YssFormilyDsl.option(SourceType.EMAIL.name(), "邮件"),
                                        YssFormilyDsl.option(SourceType.S3.name(), "S3"),
                                        YssFormilyDsl.option(SourceType.SFTP.name(), "SFTP")
                                )
                                .gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.SOURCE_CODE, "来源编码").required().placeholder("例如：source-default").gridSpan(2),
                        YssFormilyDsl.inputNumber("ruleId", "规则主键").required().placeholder("请输入规则主键").gridSpan(1),
                        YssFormilyDsl.groupHeader("header2", "目标信息"),
                        YssFormilyDsl.select(TransferConfigKeys.TARGET_TYPE, "目标类型")
                                .required()
                                .options(
                                        YssFormilyDsl.option(TargetType.EMAIL.name(), "邮件"),
                                        YssFormilyDsl.option(TargetType.S3.name(), "S3"),
                                        YssFormilyDsl.option(TargetType.SFTP.name(), "SFTP"),
                                        YssFormilyDsl.option(TargetType.FILESYS.name(), "文件服务")
                                )
                                .gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.TARGET_CODE, "目标编码").required().placeholder("例如：filesys-archive").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.TARGET_PATH, "目标路径").placeholder("例如：/transfer/inbox").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.RENAME_PATTERN, "重命名模板").placeholder("${fileName}").gridSpan(1),
                        YssFormilyDsl.select("routeStatus", "路由状态")
                                .required()
                                .options(
                                        YssFormilyDsl.option(TransferStatus.PENDING.name(), "待处理"),
                                        YssFormilyDsl.option(TransferStatus.ROUTED.name(), "已路由"),
                                        YssFormilyDsl.option(TransferStatus.FAILED.name(), "失败"),
                                        YssFormilyDsl.option(TransferStatus.SKIPPED.name(), "已跳过"),
                                        YssFormilyDsl.option(TransferStatus.QUARANTINED.name(), "隔离")
                                )
                                .gridSpan(1),
                        YssFormilyDsl.groupHeader("header3", "投递策略"),
                        YssFormilyDsl.inputNumber(TransferConfigKeys.MAX_RETRY_COUNT, "最大重试次数").placeholder("3").gridSpan(1),
                        YssFormilyDsl.inputNumber(TransferConfigKeys.RETRY_DELAY_SECONDS, "重试间隔(秒)").placeholder("60").gridSpan(1),
                        YssFormilyDsl.select(TransferConfigKeys.GROUP_STRATEGY, "分组策略")
                                .required()
                                .options(
                                        YssFormilyDsl.option(TransferRouteGroupStrategy.NONE.name(), "不分组"),
                                        YssFormilyDsl.option(TransferRouteGroupStrategy.FILE_TYPE.name(), "按文件类型"),
                                        YssFormilyDsl.option(TransferRouteGroupStrategy.FILE_NAME.name(), "按文件名称"),
                                        YssFormilyDsl.option(TransferRouteGroupStrategy.MAIL_FROM.name(), "按邮件发件人"),
                                        YssFormilyDsl.option(TransferRouteGroupStrategy.MAIL_TO.name(), "按邮件收件人"),
                                        YssFormilyDsl.option(TransferRouteGroupStrategy.CUSTOM.name(), "自定义字段")
                                )
                                .gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.GROUP_FIELD, "分组字段").placeholder("自定义分组时使用").gridSpan(1),
                        YssFormilyDsl.input(TransferConfigKeys.DEFAULT_TARGET_CODE, "默认目标编码").placeholder("可选").gridSpan(1),
                        YssFormilyDsl.textArea(TransferConfigKeys.GROUP_TARGET_MAPPING, "分组目标映射")
                                .placeholder("{\"finance@example.com\":\"filesys-finance\"}")
                                .gridSpan(2)
                )
                .build();
    }
}
