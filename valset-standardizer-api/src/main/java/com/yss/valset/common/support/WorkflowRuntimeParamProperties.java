package com.yss.valset.common.support;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 工作流运行参数默认值。
 */
@Data
@Component
public class WorkflowRuntimeParamProperties {

    /**
     * 是否跳过 Excel 样式解析。
     */
    @Value("${subject.match.workflow.skip-excel-style-parsing:false}")
    private boolean skipExcelStyleParsing;

    /**
     * 是否启用匹配流程。
     */
    @Value("${subject.match.workflow.enable-match-process:true}")
    private boolean enableMatchProcess;

}
