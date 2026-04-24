package com.yss.valset.transfer.web.controller;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.transfer.application.command.TransferSourceUpsertCommand;
import com.yss.valset.transfer.application.dto.TransferSourceCheckpointItemViewDTO;
import com.yss.valset.transfer.application.dto.TransferSourceCheckpointViewDTO;
import com.yss.valset.transfer.application.dto.TransferSourceMutationResponse;
import com.yss.valset.transfer.application.dto.TransferSourceViewDTO;
import com.yss.valset.transfer.application.service.TransferIngestProgressAppService;
import com.yss.valset.transfer.application.service.TransferSourceManagementAppService;
import com.yss.valset.transfer.domain.form.TransferFormTemplateNames;
import com.yss.valset.transfer.domain.model.SourceType;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.format.DateTimeFormatter;

/**
 * 文件来源管理接口。
 */
@RestController
@RequestMapping({"/api/transfer-sources", "/transfer-sources"})
public class TransferSourceController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TransferSourceManagementAppService transferSourceManagementAppService;
    private final TransferIngestProgressAppService transferIngestProgressAppService;

    public TransferSourceController(TransferSourceManagementAppService transferSourceManagementAppService,
                                    TransferIngestProgressAppService transferIngestProgressAppService) {
        this.transferSourceManagementAppService = transferSourceManagementAppService;
        this.transferIngestProgressAppService = transferIngestProgressAppService;
    }

    /**
     * 查询文件来源列表。
     *
     * @param sourceType 来源类型
     * @param sourceCode 来源编码
     * @param sourceName 来源名称
     * @param enabled 是否启用
     * @param limit 查询上限
     * @return 文件来源列表
     */
    @GetMapping
    @Operation(summary = "查询文件来源列表", description = "按来源类型、来源编码、来源名称和启用状态查询文件来源。")
    public MultiResult<TransferSourceViewDTO> listSources(@RequestParam(value = "sourceType", required = false) String sourceType,
                                                          @RequestParam(value = "sourceCode", required = false) String sourceCode,
                                                          @RequestParam(value = "sourceName", required = false) String sourceName,
                                                          @RequestParam(value = "enabled", required = false) Boolean enabled,
                                                          @RequestParam(value = "limit", required = false) Integer limit) {
        return MultiResult.of(transferSourceManagementAppService.listSources(sourceType, sourceCode, sourceName, enabled, limit));
    }

    /**
     * 查询文件来源详情。
     *
     * @param sourceId 来源主键
     * @return 文件来源详情
     */
    @GetMapping("/{sourceId}")
    @Operation(summary = "查询文件来源详情")
    public SingleResult<TransferSourceViewDTO> getSource(@PathVariable String sourceId) {
        return SingleResult.of(transferSourceManagementAppService.getSource(sourceId));
    }

    /**
     * 查询来源对应表单模板名。
     *
     * @param sourceType 来源类型
     * @return 表单模板名称
     */
    @GetMapping("/template-name")
    @Operation(summary = "查询来源对应表单模板名", description = "根据来源类型返回前端应加载的表单模板名称。")
    public SingleResult<String> getTemplateName(@RequestParam("sourceType") SourceType sourceType) {
        return SingleResult.of(TransferFormTemplateNames.sourceTemplateName(sourceType));
    }

    /**
     * 创建文件来源。
     *
     * @param command 文件来源新增命令
     * @return 文件来源变更结果
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建文件来源", description = "创建新的本地目录、邮箱、S3 或 SFTP 文件来源配置。")
    public SingleResult<TransferSourceMutationResponse> createSource(@Valid @RequestBody TransferSourceUpsertCommand command) {
        command.setSourceId(null);
        return SingleResult.of(transferSourceManagementAppService.upsertSource(command));
    }

    /**
     * 更新文件来源。
     *
     * @param sourceId 来源主键
     * @param command 文件来源更新命令
     * @return 文件来源变更结果
     */
    @PutMapping("/{sourceId}")
    @Operation(summary = "更新文件来源", description = "更新文件来源配置与连接参数。")
    public SingleResult<TransferSourceMutationResponse> updateSource(@PathVariable String sourceId,
                                                                     @Valid @RequestBody TransferSourceUpsertCommand command) {
        command.setSourceId(sourceId);
        return SingleResult.of(transferSourceManagementAppService.upsertSource(command));
    }

    /**
     * 删除文件来源。
     *
     * @param sourceId 来源主键
     * @return 文件来源变更结果
     */
    @DeleteMapping("/{sourceId}")
    @Operation(summary = "删除文件来源", description = "按来源 id 删除文件来源配置；如果已有分拣路由引用该来源，则拒绝删除。")
    public SingleResult<TransferSourceMutationResponse> deleteSource(@PathVariable String sourceId) {
        return SingleResult.of(transferSourceManagementAppService.deleteSource(sourceId));
    }

    /**
     * 立即触发一次文件来源收取。
     *
     * @param sourceId 来源主键
     * @return 文件来源变更结果
     */
    @PostMapping("/{sourceId}/trigger")
    @Operation(summary = "立即触发文件来源收取", description = "根据来源配置立即执行一次收取，然后继续完成规则识别、路由和投递。")
    public SingleResult<TransferSourceMutationResponse> triggerSource(@PathVariable String sourceId) {
        return SingleResult.of(transferSourceManagementAppService.triggerSource(sourceId));
    }

    /**
     * 停止文件来源收取。
     *
     * @param sourceId 来源主键
     * @return 文件来源变更结果
     */
    @PostMapping("/{sourceId}/stop")
    @Operation(summary = "停止文件来源收取", description = "停止当前正在执行的文件来源收取，并让正在运行的任务在下一次检查点协作退出。")
    public SingleResult<TransferSourceMutationResponse> stopSource(@PathVariable String sourceId) {
        return SingleResult.of(transferSourceManagementAppService.stopSource(sourceId));
    }

    /**
     * 清空来源检查点记录。
     *
     * @param sourceId 来源主键
     * @return 文件来源变更结果
     */
    @PostMapping("/{sourceId}/checkpoint/processed-mail-ids/clear")
    @Operation(summary = "清空来源检查点记录", description = "清空该来源 checkpoint 表中的扫描游标和去重记录，便于重新收取历史邮件。")
    public SingleResult<TransferSourceMutationResponse> clearProcessedMailIds(@PathVariable String sourceId) {
        return SingleResult.of(transferSourceManagementAppService.clearProcessedMailIds(sourceId));
    }

    /**
     * 查询来源检查点列表。
     *
     * @param sourceId 来源主键
     * @param limit 查询上限
     * @return 来源检查点列表
     */
    @GetMapping("/{sourceId}/checkpoints")
    @Operation(summary = "查询来源检查点列表", description = "查询该来源当前保存的扫描游标与相关检查点记录。")
    public MultiResult<TransferSourceCheckpointViewDTO> listCheckpoints(@PathVariable String sourceId,
                                                                        @RequestParam(value = "limit", required = false) Integer limit) {
        return MultiResult.of(transferSourceManagementAppService.listCheckpoints(sourceId, limit));
    }

    /**
     * 查询来源检查点去重记录。
     *
     * @param sourceId 来源主键
     * @param limit 查询上限
     * @return 来源检查点去重记录列表
     */
    @GetMapping("/{sourceId}/checkpoint-items")
    @Operation(summary = "查询来源检查点去重记录", description = "查询该来源已经处理过的条目记录。")
    public MultiResult<TransferSourceCheckpointItemViewDTO> listCheckpointItems(@PathVariable String sourceId,
                                                                                @RequestParam(value = "limit", required = false) Integer limit) {
        return MultiResult.of(transferSourceManagementAppService.listCheckpointItems(sourceId, limit));
    }

    /**
     * 订阅来源收取进度。
     *
     * @param sourceId 来源主键
     * @return SSE 连接
     */
    @GetMapping(value = "/{sourceId}/progress/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅来源收取进度", description = "通过 SSE 持续推送指定来源的收取状态、进度、完成和错误事件。")
    public SseEmitter subscribeProgress(@PathVariable String sourceId) {
        TransferSourceViewDTO source = transferSourceManagementAppService.getSource(sourceId);
        SseEmitter emitter = transferIngestProgressAppService.subscribe(sourceId);
        transferIngestProgressAppService.publishStatus(
                sourceId,
                normalizeInitialStatus(source),
                buildInitialStatusMessage(source),
                source == null ? null : source.getIngestTriggerType(),
                source == null || source.getIngestStartedAt() == null ? null : source.getIngestStartedAt().toString()
        );
        if (Boolean.TRUE.equals(source.getIngestBusy())) {
            transferIngestProgressAppService.publishProgress(sourceId, 0L, 0L, "收取已开始，等待首个文件进度");
        }
        return emitter;
    }

    private String normalizeInitialStatus(TransferSourceViewDTO source) {
        if (source == null || source.getIngestStatus() == null || source.getIngestStatus().isBlank()) {
            return Boolean.TRUE.equals(source == null ? null : source.getIngestBusy()) ? "running" : "idle";
        }
        return source.getIngestStatus().trim().toLowerCase();
    }

    private String buildInitialStatusMessage(TransferSourceViewDTO source) {
        if (source == null) {
            return "来源状态未知";
        }
        if (Boolean.TRUE.equals(source.getIngestBusy())) {
            String triggerType = describeTriggerType(source.getIngestTriggerType());
            String triggerTime = formatTriggerTime(source.getIngestStartedAt());
            return "来源当前正在收取中，触发方式：" + triggerType + "，触发时间：" + triggerTime;
        }
        if (source.getIngestStatus() == null || source.getIngestStatus().isBlank()) {
            return "来源当前待运行";
        }
        if (source.getIngestTriggerType() != null && !source.getIngestTriggerType().isBlank()) {
            String triggerTime = formatTriggerTime(source.getIngestStartedAt());
            return "来源当前状态：" + source.getIngestStatus() + "，最近触发方式：" + describeTriggerType(source.getIngestTriggerType()) + "，触发时间：" + triggerTime;
        }
        return "来源当前状态：" + source.getIngestStatus();
    }

    private String formatTriggerTime(java.time.LocalDateTime triggerTime) {
        if (triggerTime == null) {
            return "-";
        }
        return TIME_FORMATTER.format(triggerTime);
    }

    private String describeTriggerType(String triggerType) {
        if (triggerType == null || triggerType.isBlank()) {
            return "未知";
        }
        String normalized = triggerType.trim().toUpperCase();
        return switch (normalized) {
            case "CRON" -> "cron 定时";
            case "MANUAL" -> "手动触发";
            case "SYSTEM" -> "系统触发";
            default -> normalized;
        };
    }
}
