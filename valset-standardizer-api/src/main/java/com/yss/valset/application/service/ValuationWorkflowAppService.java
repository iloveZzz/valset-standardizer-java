package com.yss.valset.application.service;

import com.yss.valset.application.command.MatchTaskCommand;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.application.dto.FullWorkflowResponse;
import com.yss.valset.application.dto.TaskViewDTO;
import com.yss.valset.application.dto.UploadValuationFileResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 外部估值全流程编排服务。
 *
 * <p>
 * 这里定义的是估值文件从上传、提取、解析到匹配的主链路入口。
 * 调用方只需要传入文件和必要参数，具体的任务创建、上下文拼装和调度触发都由实现类完成。
 * </p>
 */
public interface ValuationWorkflowAppService {
    /**
     * 上传估值文件并完成 ODS 原始提取。
     *
     * @param file 原始估值文件。
     * @param dataSourceType 数据源类型，用于区分文件来源和解析路径。
     * @param createdBy 创建人标识。
     * @param forceRebuild 是否强制重建，true 时忽略可复用任务。
     */
    UploadValuationFileResponse uploadAndExtract(MultipartFile file, String dataSourceType, String createdBy, Boolean forceRebuild);

    /**
     * 执行 STG 解析落地。
     *
     * @param command 解析任务参数，包含文件、工作簿、执行上下文等信息。
     */
    TaskViewDTO analyze(ParseTaskCommand command);

    /**
     * 执行外部估值匹配。
     *
     * @param command 匹配任务参数，通常包含文件标识、工作簿路径和 topK 等参数。
     */
    TaskViewDTO match(MatchTaskCommand command);

    /**
     * 上传文件并串联执行提取、解析、匹配。
     *
     * @param file 原始估值文件。
     * @param dataSourceType 数据源类型。
     * @param topK 匹配阶段保留的候选数量。
     * @param createdBy 创建人标识。
     * @param forceRebuild 是否强制重建所有阶段任务。
     */
    FullWorkflowResponse runFullWorkflow(MultipartFile file,
                                         String dataSourceType,
                                         Integer topK,
                                         String createdBy,
                                         Boolean forceRebuild);
}
