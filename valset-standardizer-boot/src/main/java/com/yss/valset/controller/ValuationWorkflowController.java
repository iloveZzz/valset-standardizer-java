package com.yss.valset.controller;

import com.yss.valset.application.command.MatchTaskCommand;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.application.dto.DwdExternalValuationViewDTO;
import com.yss.valset.application.dto.FullWorkflowResponse;
import com.yss.valset.application.dto.MatchResultViewDTO;
import com.yss.valset.application.dto.RawValuationDataViewDTO;
import com.yss.valset.application.dto.TaskViewDTO;
import com.yss.valset.application.dto.UploadValuationFileResponse;
import com.yss.valset.application.dto.StgExternalValuationViewDTO;
import com.yss.valset.application.service.ValuationWorkflowAppService;
import com.yss.valset.application.service.ValuationWorkflowQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 外部估值全流程接口。
 */
@RestController
@RequestMapping("/api/valuation-workflows")
public class ValuationWorkflowController {

    private final ValuationWorkflowAppService valuationWorkflowAppService;
    private final ValuationWorkflowQueryService valuationWorkflowQueryService;

    public ValuationWorkflowController(ValuationWorkflowAppService valuationWorkflowAppService,
                                       ValuationWorkflowQueryService valuationWorkflowQueryService) {
        this.valuationWorkflowAppService = valuationWorkflowAppService;
        this.valuationWorkflowQueryService = valuationWorkflowQueryService;
    }

    /**
     * 上传外部估值表并完成 ODS 原始提取。
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "上传外部估值表并执行 ODS 原始提取",
            description = "上传成功后系统会先将文件落盘，再同步执行 ODS 原始行抽取，并返回 fileId 供后续 analyze/match/full-process 查询使用。"
    )
    public UploadValuationFileResponse upload(@RequestPart("file") MultipartFile file,
                                              @RequestParam(value = "dataSourceType", required = false) String dataSourceType,
                                              @RequestParam(value = "createdBy", required = false) String createdBy,
                                              @RequestParam(value = "forceRebuild", required = false, defaultValue = "false") Boolean forceRebuild) {
        return valuationWorkflowAppService.uploadAndExtract(file, dataSourceType, createdBy, forceRebuild);
    }

    /**
     * 执行 DWD 外部估值解析。
     */
    @PostMapping("/analyze")
    @Operation(summary = "基于 ODS 原始数据生成 DWD 外部估值标准数据", description = "Excel/CSV 场景必须传入 fileId，系统会读取 ODS 原始行数据并落地 DWD 标准表。")
    public TaskViewDTO analyze(@org.springframework.web.bind.annotation.RequestBody ParseTaskCommand command) {
        return valuationWorkflowAppService.analyze(command);
    }

    /**
     * 执行外部估值科目匹配。
     */
    @PostMapping("/match")
    @Operation(summary = "外部估值明细与内部标准科目匹配打标", description = "Excel/CSV 场景必须传入 fileId，匹配阶段会优先读取 DWD 标准表数据。")
    public TaskViewDTO match(@org.springframework.web.bind.annotation.RequestBody MatchTaskCommand command) {
        return valuationWorkflowAppService.match(command);
    }

    /**
     * 上传文件并串联执行提取、解析、匹配。
     */
    @PostMapping(value = "/full-process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件并同步执行提取、解析、匹配全流程", description = "一步完成上传、ODS 抽取、DWD 标准化落地和外部科目匹配。")
    public FullWorkflowResponse fullProcess(@RequestPart("file") MultipartFile file,
                                            @RequestParam(value = "dataSourceType", required = false) String dataSourceType,
                                            @RequestParam(value = "topK", required = false) Integer topK,
                                            @RequestParam(value = "createdBy", required = false) String createdBy,
                                            @RequestParam(value = "forceRebuild", required = false, defaultValue = "false") Boolean forceRebuild) {
        return valuationWorkflowAppService.runFullWorkflow(
                file,
                dataSourceType,
                topK,
                createdBy,
                forceRebuild
        );
    }

    /**
     * 查询 ODS 原始行数据。
     */
    @GetMapping("/{fileId}/raw-data")
    @Operation(summary = "查询 ODS 原始外部估值数据", description = "按 fileId 查询落入 t_ods_valuation_filedata 的原始行数据。")
    public RawValuationDataViewDTO queryRawData(@PathVariable Long fileId,
                                                @RequestParam(value = "limit", required = false) Integer limit) {
        return valuationWorkflowQueryService.queryRawData(fileId, limit);
    }

    /**
     * 查询 STG 外部估值解析快照。
     */
    @GetMapping("/{fileId}/stg-data")
    @Operation(summary = "查询 STG 外部估值解析快照", description = "按 fileId 聚合查询 STG 外部估值主表、基础信息、表头、明细和指标数据。")
    public StgExternalValuationViewDTO queryStgData(@PathVariable Long fileId) {
        return valuationWorkflowQueryService.queryStgData(fileId);
    }

    /**
     * 查询 DWD 外部估值标准数据。
     */
    @GetMapping("/{fileId}/dwd-data")
    @Operation(summary = "查询 DWD 外部估值标准数据", description = "按 fileId 聚合查询 DWD 外部估值主表、基础信息、表头、明细和指标数据。")
    public DwdExternalValuationViewDTO queryDwdData(@PathVariable Long fileId) {
        return valuationWorkflowQueryService.queryDwdData(fileId);
    }

    /**
     * 查询匹配结果。
     */
    @GetMapping("/{fileId}/match-results")
    @Operation(summary = "查询外部估值科目匹配结果", description = "按 fileId 查询外部估值明细与内部标准科目的匹配打标结果。")
    public MatchResultViewDTO queryMatchResults(@PathVariable Long fileId) {
        return valuationWorkflowQueryService.queryMatchResults(fileId);
    }
}
