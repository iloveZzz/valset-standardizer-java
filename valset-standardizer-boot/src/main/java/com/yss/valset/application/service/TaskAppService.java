package com.yss.valset.application.service;

import com.yss.valset.application.command.EvaluateMappingTaskCommand;
import com.yss.valset.extract.application.command.ExtractDataTaskCommand;
import com.yss.valset.application.command.MatchTaskCommand;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.application.dto.TaskCreateResponse;

/**
 * 用于创建主题匹配任务的应用程序服务。
 */
public interface TaskAppService {
    /**
     * 为工作簿创建解析任务。
     */
    TaskCreateResponse createParseTask(ParseTaskCommand command);

    /**
     * 创建匹配任务。
     */
    TaskCreateResponse createMatchTask(MatchTaskCommand command);

    /**
     * 创建离线评估任务。
     */
    TaskCreateResponse createEvaluateTask(EvaluateMappingTaskCommand command);

    /**
     * 创建原始数据提取任务。
     */
    TaskCreateResponse createExtractTask(ExtractDataTaskCommand command);
}
