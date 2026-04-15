package com.yss.subjectmatch.controller;

import com.yss.subjectmatch.application.dto.SubjectMatchFileIngestLogViewDTO;
import com.yss.subjectmatch.application.dto.SubjectMatchFileInfoViewDTO;
import com.yss.subjectmatch.application.dto.UploadValuationFileResponse;
import com.yss.subjectmatch.application.dto.ValuationSheetStyleViewDTO;
import com.yss.subjectmatch.application.service.FileManagementQueryAppService;
import com.yss.subjectmatch.application.service.ValuationWorkflowAppService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件信息管理接口。
 */
@RestController
@RequestMapping("/api/files")
public class FileManagementController {

    private final ValuationWorkflowAppService valuationWorkflowAppService;
    private final FileManagementQueryAppService fileManagementQueryAppService;

    public FileManagementController(ValuationWorkflowAppService valuationWorkflowAppService,
                                    FileManagementQueryAppService fileManagementQueryAppService) {
        this.valuationWorkflowAppService = valuationWorkflowAppService;
        this.fileManagementQueryAppService = fileManagementQueryAppService;
    }

    /**
     * 手动上传文件并执行 ODS 提取。
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "手动上传文件并执行 ODS 提取", description = "与 valuation-workflows/upload 等价，但归属于文件管理入口。")
    public UploadValuationFileResponse upload(@RequestPart("file") MultipartFile file,
                                              @RequestParam(value = "dataSourceType", required = false) String dataSourceType,
                                              @RequestParam(value = "createdBy", required = false) String createdBy,
                                              @RequestParam(value = "forceRebuild", required = false, defaultValue = "false") Boolean forceRebuild) {
        return valuationWorkflowAppService.uploadAndExtract(file, dataSourceType, createdBy, forceRebuild);
    }

    /**
     * 查询文件主数据。
     */
    @GetMapping("/{fileId}")
    @Operation(summary = "查询文件主数据")
    public SubjectMatchFileInfoViewDTO queryFileInfo(@PathVariable Long fileId) {
        return fileManagementQueryAppService.queryFileInfo(fileId);
    }

    /**
     * 按条件搜索文件主数据。
     */
    @GetMapping
    @Operation(summary = "搜索文件主数据", description = "支持按 sourceChannel、fileStatus、fingerprint 搜索，limit 默认 50。")
    public List<SubjectMatchFileInfoViewDTO> searchFileInfos(@RequestParam(value = "sourceChannel", required = false) String sourceChannel,
                                                             @RequestParam(value = "fileStatus", required = false) String fileStatus,
                                                             @RequestParam(value = "fingerprint", required = false) String fileFingerprint,
                                                             @RequestParam(value = "limit", required = false) Integer limit) {
        return fileManagementQueryAppService.searchFileInfos(sourceChannel, fileStatus, fileFingerprint, limit);
    }

    /**
     * 查询文件接入日志。
     */
    @GetMapping("/{fileId}/ingest-logs")
    @Operation(summary = "查询文件接入日志")
    public List<SubjectMatchFileIngestLogViewDTO> queryIngestLogs(@PathVariable Long fileId) {
        return fileManagementQueryAppService.queryIngestLogs(fileId);
    }

    /**
     * 查询文件对应的 Excel sheet 样式快照。
     */
    @GetMapping("/{fileId}/sheet-styles")
    @Operation(summary = "查询文件对应的 Excel sheet 样式快照",
            description = "仅 Excel 文件会写入 sheet 样式快照，返回标题、header 与合并区域相关的 Univer 结构。")
    public List<ValuationSheetStyleViewDTO> querySheetStyles(@PathVariable Long fileId) {
        return fileManagementQueryAppService.querySheetStyles(fileId);
    }
}
