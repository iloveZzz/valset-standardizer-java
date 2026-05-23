package com.yss.valset.application.support;

import com.yss.valset.application.command.MatchTaskCommand;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.application.dto.workflow.WorkflowContextKeys;
import com.yss.valset.extract.application.command.ExtractDataTaskCommand;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作流通用上下文构建器。
 *
 * <p>
 * 负责把和业务类型无关的公共参数拼到工作流上下文里，例如创建人和是否强制重建。
 * </p>
 */
@Component
public class WorkflowCommonContextBuilder {

    /**
     * 从解析任务参数中提取公共上下文。
     */
    public Map<String, Object> build(ParseTaskCommand command) {
        return buildCommon(command == null ? null : command.getCreatedBy(),
                command == null ? null : command.getForceRebuild());
    }

    /**
     * 从匹配任务参数中提取公共上下文。
     */
    public Map<String, Object> build(MatchTaskCommand command) {
        return buildCommon(command == null ? null : command.getCreatedBy(),
                command == null ? null : command.getForceRebuild());
    }

    /**
     * 从文件提取任务参数中提取公共上下文。
     */
    public Map<String, Object> build(ExtractDataTaskCommand command) {
        return buildCommon(command == null ? null : command.getCreatedBy(),
                command == null ? null : command.getForceRebuild());
    }

    /**
     * 公共上下文只保留最稳定、最基础的任务级参数。
     */
    private Map<String, Object> buildCommon(String createdBy, Boolean forceRebuild) {
        Map<String, Object> context = new LinkedHashMap<>();
        put(context, WorkflowContextKeys.CREATED_BY, text(createdBy));
        put(context, WorkflowContextKeys.FORCE_REBUILD, forceRebuild);
        return context;
    }

    private void put(Map<String, Object> context, String key, Object value) {
        context.put(key, value);
    }

    private String text(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
