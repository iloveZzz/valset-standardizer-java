package com.yss.valset.controller;

import com.yss.cloud.dto.response.SingleResult;
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
@RequestMapping("/valuation-workflows")
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
     *
     * @param file 上传的外部估值文件
     * @param dataSourceType 数据源类型
     * @param createdBy 创建人
     * @param forceRebuild 是否强制重建
     * @return 上传结果
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "上传外部估值表并执行 ODS 原始提取",
            description = "上传成功后系统会先将文件落盘，再同步执行 ODS 原始行抽取，并返回 fileId 供后续 analyze/match/full-process 查询使用。"
    )
    public SingleResult<UploadValuationFileResponse> upload(@RequestPart("file") MultipartFile file,
                                                            @RequestParam(value = "dataSourceType", required = false) String dataSourceType,
                                                            @RequestParam(value = "createdBy", required = false) String createdBy,
                                                            @RequestParam(value = "forceRebuild", required = false, defaultValue = "false") Boolean forceRebuild) {
        return SingleResult.of(valuationWorkflowAppService.uploadAndExtract(file, dataSourceType, createdBy, forceRebuild));
    }

    /**
     * 执行 DWD 外部估值解析。
     *
     * @param command 解析任务请求
     * @return 解析任务结果
     */
    @PostMapping("/analyze")
    @Operation(summary = "基于 ODS 原始数据生成 DWD 外部估值标准数据", description = "Excel/CSV 场景必须传入 fileId，系统会读取 ODS 原始行数据并落地 DWD 标准表。")
    public SingleResult<TaskViewDTO> analyze(@org.springframework.web.bind.annotation.RequestBody ParseTaskCommand command) {
        return SingleResult.of(valuationWorkflowAppService.analyze(command));
    }

    /**
     * 执行外部估值科目匹配。
     *
     * @param command 匹配任务请求
     * @return 匹配任务结果
     */
    @PostMapping("/match")
    @Operation(summary = "外部估值明细与内部标准科目匹配打标", description = "Excel/CSV 场景必须传入 fileId，匹配阶段会优先读取 DWD 标准表数据。")
    public SingleResult<TaskViewDTO> match(@org.springframework.web.bind.annotation.RequestBody MatchTaskCommand command) {
        return SingleResult.of(valuationWorkflowAppService.match(command));
    }

    /**
     * 上传文件并串联执行提取、解析、匹配。
     *
     * @param file 上传的外部估值文件
     * @param dataSourceType 数据源类型
     * @param topK 匹配候选数量
     * @param createdBy 创建人
     * @param forceRebuild 是否强制重建
     * @return 全流程执行结果
     */
    @PostMapping(value = "/full-process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件并同步执行提取、解析、匹配全流程", description = "一步完成上传、ODS 抽取、DWD 标准化落地和外部科目匹配。")
    public SingleResult<FullWorkflowResponse> fullProcess(@RequestPart("file") MultipartFile file,
                                                           @RequestParam(value = "dataSourceType", required = false) String dataSourceType,
                                                           @RequestParam(value = "topK", required = false) Integer topK,
                                                           @RequestParam(value = "createdBy", required = false) String createdBy,
                                                           @RequestParam(value = "forceRebuild", required = false, defaultValue = "false") Boolean forceRebuild) {
        return SingleResult.of(valuationWorkflowAppService.runFullWorkflow(
                file,
                dataSourceType,
                topK,
                createdBy,
                forceRebuild
        ));
    }

    /**
     * 查询 ODS 原始行数据。
     *
     * @param fileId 文件主键
     * @param limit 查询上限
     * @return ODS 原始行数据
     */
    @GetMapping("/{fileId}/raw-data")
    @Operation(summary = "查询 ODS 原始外部估值数据", description = "按 fileId 查询落入 t_ods_valuation_filedata 的原始行数据。")
    public SingleResult<RawValuationDataViewDTO> queryRawData(@PathVariable Long fileId,
                                                              @RequestParam(value = "limit", required = false) Integer limit) {
        return SingleResult.of(valuationWorkflowQueryService.queryRawData(fileId, limit));
    }

    /**
     * 查询 STG 外部估值解析快照。
     *
     * @param fileId 文件主键
     * @return STG 解析快照
     */
    @GetMapping("/{fileId}/stg-data")
    @Operation(summary = "查询 STG 外部估值解析快照", description = "按 fileId 聚合查询 STG 外部估值主表、基础信息、表头、明细和指标数据。")
    public SingleResult<StgExternalValuationViewDTO> queryStgData(@PathVariable Long fileId) {
        return SingleResult.of(valuationWorkflowQueryService.queryStgData(fileId));
    }

    /**
     * 查询 DWD 外部估值标准数据。
     *
     * @param fileId 文件主键
     * @return DWD 标准数据
     */
    @GetMapping("/{fileId}/dwd-data")
    @Operation(summary = "查询 DWD 外部估值标准数据", description = "按 fileId 聚合查询 DWD 外部估值主表、基础信息、表头、明细和指标数据。")
    public SingleResult<DwdExternalValuationViewDTO> queryDwdData(@PathVariable Long fileId) {
        return SingleResult.of(valuationWorkflowQueryService.queryDwdData(fileId));
    }

    /**
     * 查询匹配结果。
     *
     * @param fileId 文件主键
     * @return 匹配结果
     */
    @GetMapping("/{fileId}/match-results")
    @Operation(summary = "查询外部估值科目匹配结果", description = "按 fileId 查询外部估值明细与内部标准科目的匹配打标结果。")
    public SingleResult<MatchResultViewDTO> queryMatchResults(@PathVariable Long fileId) {
        return SingleResult.of(valuationWorkflowQueryService.queryMatchResults(fileId));
    }
}
