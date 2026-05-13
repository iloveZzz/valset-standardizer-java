package com.yss.valset.application.dto.workflow;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作流执行上下文。
 *
 * <p>
 * 这个对象承载一次任务执行所需的全部上下文信息，主要分成三类：
 * </p>
 * <ul>
 *     <li>工作流自身信息：工作流编号、版本、阶段、引擎类型。</li>
 *     <li>通用上下文：任务级别都会携带的公共参数。</li>
 *     <li>业务上下文：和估值文件、解析参数、匹配参数直接相关的业务参数。</li>
 * </ul>
 */
@Data
public class WorkflowExecutionContextDTO implements java.io.Serializable {

    /** 工作流唯一标识。 */
    private String workflowId;
    /** 工作流编码。 */
    private String workflowCode;
    /** 工作流版本号。 */
    private Integer workflowVersionNo;
    /** 当前执行阶段编码。 */
    private String workflowStageCode;
    /** 当前执行阶段名称。 */
    private String workflowStageName;
    /** 当前执行阶段说明。 */
    private String workflowStageDescription;
    /** 工作流引擎类型，当前默认是内部调度或内部工作流实现。 */
    private String engineType;
    /** 外部系统关联标识，用于对接第三方编排器或平台。 */
    private String externalRef;
    /** 工作流配置原始 JSON。 */
    private String configJson;
    /** 工作流绑定标识。 */
    private String bindingId;
    /** 当前上下文是否已经成功绑定到具体工作流定义。 */
    private Boolean bindingResolved;

    /** 通用上下文，存放任务创建人、是否强制重建等公共参数。 */
    private Map<String, Object> commonContext = new LinkedHashMap<>();
    /** 业务上下文的原始 JSON，通常用于透传和排障。 */
    private String businessContextJson;

    /** 业务上下文展开后的键值对，供执行器直接读取。 */
    private Map<String, Object> businessContext = new LinkedHashMap<>();
}
