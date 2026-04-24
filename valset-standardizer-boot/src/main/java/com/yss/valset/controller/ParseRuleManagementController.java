package com.yss.valset.controller;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.application.command.ParseRuleProfileUpsertCommand;
import com.yss.valset.application.command.ParseRulePublishCommand;
import com.yss.valset.application.command.ParseRuleRollbackCommand;
import com.yss.valset.application.dto.ParseRuleBundleViewDTO;
import com.yss.valset.application.dto.ParseRuleMutationResponse;
import com.yss.valset.application.dto.ParseRuleProfileViewDTO;
import com.yss.valset.application.dto.ParseRuleRegressionViewDTO;
import com.yss.valset.application.dto.ParseRuleTraceViewDTO;
import com.yss.valset.application.dto.ParseRuleValidationViewDTO;
import com.yss.valset.application.service.ParseRuleManagementAppService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * 解析模板管理接口。
 */
@RestController
@RequestMapping("/api/parse-rules")
public class ParseRuleManagementController {

    private final ParseRuleManagementAppService parseRuleManagementAppService;

    public ParseRuleManagementController(ParseRuleManagementAppService parseRuleManagementAppService) {
        this.parseRuleManagementAppService = parseRuleManagementAppService;
    }

    /**
     * 查询解析模板列表。
     *
     * @param status 模板状态
     * @param profileCode 模板编码
     * @param limit 查询上限
     * @return 模板列表
     */
    @GetMapping("/profiles")
    @Operation(summary = "查询解析模板列表", description = "按状态和模板编码过滤查询解析模板列表。")
    public MultiResult<ParseRuleProfileViewDTO> listProfiles(@RequestParam(value = "status", required = false) String status,
                                                             @RequestParam(value = "profileCode", required = false) String profileCode,
                                                             @RequestParam(value = "limit", required = false) Integer limit) {
        return MultiResult.of(parseRuleManagementAppService.listProfiles(status, profileCode, limit));
    }

    /**
     * 查询解析模板详情。
     *
     * @param profileId 模板主键
     * @return 模板详情
     */
    @GetMapping("/profiles/{profileId}")
    @Operation(summary = "查询解析模板详情", description = "查询模板主表、规则步骤、样例和发布日志。")
    public SingleResult<ParseRuleBundleViewDTO> getProfile(@PathVariable Long profileId) {
        return SingleResult.of(parseRuleManagementAppService.getProfile(profileId));
    }

    /**
     * 创建解析模板。
     *
     * @param command 模板新增命令
     * @return 模板变更结果
     */
    @PostMapping("/profiles")
    @Operation(summary = "创建解析模板", description = "新增一套解析模板草稿，并同步写入规则步骤和样例。")
    public SingleResult<ParseRuleMutationResponse> createProfile(@RequestBody ParseRuleProfileUpsertCommand command) {
        return SingleResult.of(parseRuleManagementAppService.upsertProfile(command));
    }

    /**
     * 更新解析模板。
     *
     * @param profileId 模板主键
     * @param command 模板更新命令
     * @return 模板变更结果
     */
    @PutMapping("/profiles/{profileId}")
    @Operation(summary = "更新解析模板", description = "更新解析模板主信息、规则步骤和样例。")
    public SingleResult<ParseRuleMutationResponse> updateProfile(@PathVariable Long profileId,
                                                                 @RequestBody ParseRuleProfileUpsertCommand command) {
        command.setId(profileId);
        return SingleResult.of(parseRuleManagementAppService.upsertProfile(command));
    }

    /**
     * 校验解析模板。
     *
     * @param profileId 模板主键
     * @return 校验结果
     */
    @PostMapping("/profiles/{profileId}/validate")
    @Operation(summary = "校验解析模板", description = "对模板的步骤、样例和表达式基础一致性进行发布前校验。")
    public SingleResult<ParseRuleValidationViewDTO> validateProfile(@PathVariable Long profileId) {
        return SingleResult.of(parseRuleManagementAppService.validateProfile(profileId));
    }

    /**
     * 执行解析模板样例回归。
     *
     * @param profileId 模板主键
     * @return 回归结果
     */
    @PostMapping("/profiles/{profileId}/regression")
    @Operation(summary = "执行解析模板样例回归", description = "基于模板绑定的样例文件执行离线回归并返回每个样例的比对结果。")
    public SingleResult<ParseRuleRegressionViewDTO> runRegression(@PathVariable Long profileId) {
        return SingleResult.of(parseRuleManagementAppService.runRegression(profileId));
    }

    /**
     * 发布解析模板。
     *
     * @param profileId 模板主键
     * @param command 发布命令
     * @return 模板变更结果
     */
    @PostMapping("/profiles/{profileId}/publish")
    @Operation(summary = "发布解析模板", description = "将模板置为已发布状态，并写入发布日志。")
    public SingleResult<ParseRuleMutationResponse> publishProfile(@PathVariable Long profileId,
                                                                  @RequestBody(required = false) ParseRulePublishCommand command) {
        return SingleResult.of(parseRuleManagementAppService.publishProfile(profileId, command == null ? new ParseRulePublishCommand() : command));
    }

    /**
     * 回滚解析模板。
     *
     * @param profileId 模板主键
     * @param command 回滚命令
     * @return 模板变更结果
     */
    @PostMapping("/profiles/{profileId}/rollback")
    @Operation(summary = "回滚解析模板", description = "回滚当前模板到指定版本或自动回退到上一条可用版本。")
    public SingleResult<ParseRuleMutationResponse> rollbackProfile(@PathVariable Long profileId,
                                                                   @RequestBody(required = false) ParseRuleRollbackCommand command) {
        return SingleResult.of(parseRuleManagementAppService.rollbackProfile(profileId, command == null ? new ParseRuleRollbackCommand() : command));
    }

    /**
     * 导入解析模板。
     *
     * @param file 上传的模板包
     * @param overwriteProfileId 覆盖的模板主键
     * @return 模板变更结果
     */
    @PostMapping(value = "/profiles/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入解析模板", description = "上传 JSON 模板包，默认作为新模板导入；如指定 overwriteProfileId，则覆盖对应模板。")
    public SingleResult<ParseRuleMutationResponse> importProfile(@RequestPart("file") MultipartFile file,
                                                                 @RequestParam(value = "overwriteProfileId", required = false) Long overwriteProfileId) {
        try {
            String bundleJson = new String(file.getBytes(), StandardCharsets.UTF_8);
            return SingleResult.of(parseRuleManagementAppService.importProfile(bundleJson, overwriteProfileId));
        } catch (Exception exception) {
            throw new IllegalStateException("导入解析模板失败", exception);
        }
    }

    /**
     * 查询解析规则追踪。
     *
     * @param profileId 模板主键
     * @param fileId 文件主键
     * @param taskId 任务主键
     * @param traceType 追踪类型
     * @param limit 查询上限
     * @return 追踪列表
     */
    @GetMapping("/traces")
    @Operation(summary = "查询解析规则追踪", description = "按模板、文件、任务和追踪类型查询规则表达式追踪明细。仅在模板显式开启追踪时才会有数据。")
    public MultiResult<ParseRuleTraceViewDTO> listTraces(@RequestParam(value = "profileId", required = false) Long profileId,
                                                         @RequestParam(value = "fileId", required = false) Long fileId,
                                                         @RequestParam(value = "taskId", required = false) Long taskId,
                                                         @RequestParam(value = "traceType", required = false) String traceType,
                                                         @RequestParam(value = "limit", required = false) Integer limit) {
        return MultiResult.of(parseRuleManagementAppService.listTraces(profileId, fileId, taskId, traceType, limit));
    }

}
