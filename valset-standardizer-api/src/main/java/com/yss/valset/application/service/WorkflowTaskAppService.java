package com.yss.valset.application.service;

import com.yss.valset.application.command.EvaluateMappingTaskCommand;
import com.yss.valset.extract.application.command.ExtractDataTaskCommand;
import com.yss.valset.application.command.MatchTaskCommand;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.application.dto.TaskCreateResponse;

/**
 * 用于创建估值标准化工作流任务的应用服务。
 *
 * <p>
 * 这一层只负责“创建任务并触发执行”，不负责具体解析逻辑。
 * 典型参数包括工作簿路径、文件标识、创建人、是否强制重建，以及评估任务所需的额外配置。
 * </p>
 */
public interface WorkflowTaskAppService {
    /**
     * 为工作簿创建解析任务。
     *
     * @param command 解析任务参数。
     */
    TaskCreateResponse createParseTask(ParseTaskCommand command);

    /**
     * 创建匹配任务。
     *
     * @param command 匹配任务参数。
     */
    TaskCreateResponse createMatchTask(MatchTaskCommand command);

    /**
     * 创建离线评估任务。
     *
     * @param command 评估任务参数，通常包含映射工作簿和标准工作簿路径。
     */
    TaskCreateResponse createEvaluateTask(EvaluateMappingTaskCommand command);

    /**
     * 创建文件解析任务。
     *
     * @param command 提取任务参数。
     */
    TaskCreateResponse createExtractTask(ExtractDataTaskCommand command);
}
