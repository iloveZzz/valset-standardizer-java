package com.yss.subjectmatch.application.service;

import com.yss.subjectmatch.application.command.EvaluateMappingTaskCommand;
import com.yss.subjectmatch.application.command.ExtractDataTaskCommand;
import com.yss.subjectmatch.application.command.MatchTaskCommand;
import com.yss.subjectmatch.application.command.ParseTaskCommand;
import com.yss.subjectmatch.application.dto.TaskCreateResponse;

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
