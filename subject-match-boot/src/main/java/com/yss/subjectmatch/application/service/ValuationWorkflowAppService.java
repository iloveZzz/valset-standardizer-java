package com.yss.subjectmatch.application.service;

import com.yss.subjectmatch.application.command.MatchTaskCommand;
import com.yss.subjectmatch.application.command.ParseTaskCommand;
import com.yss.subjectmatch.application.dto.FullWorkflowResponse;
import com.yss.subjectmatch.application.dto.TaskViewDTO;
import com.yss.subjectmatch.application.dto.UploadValuationFileResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 外部估值全流程编排服务。
 */
public interface ValuationWorkflowAppService {
    /**
     * 上传估值文件并完成 ODS 原始提取。
     */
    UploadValuationFileResponse uploadAndExtract(MultipartFile file, String dataSourceType, String createdBy, Boolean forceRebuild);

    /**
     * 执行 DWD 解析落地。
     */
    TaskViewDTO analyze(ParseTaskCommand command);

    /**
     * 执行外部估值匹配。
     */
    TaskViewDTO match(MatchTaskCommand command);

    /**
     * 上传文件并串联执行提取、解析、匹配。
     */
    FullWorkflowResponse runFullWorkflow(MultipartFile file,
                                         String dataSourceType,
                                         Integer topK,
                                         String createdBy,
                                         Boolean forceRebuild);
}
