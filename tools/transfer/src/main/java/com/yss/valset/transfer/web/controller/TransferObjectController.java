package com.yss.valset.transfer.web.controller;

import com.yss.cloud.dto.response.PageResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.transfer.application.dto.TransferObjectDownloadViewDTO;
import com.yss.valset.transfer.application.command.TransferObjectRetagCommand;
import com.yss.valset.transfer.application.command.TransferObjectRedeliverCommand;
import com.yss.valset.transfer.application.dto.TransferMailInfoViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectRetagResponse;
import com.yss.valset.transfer.application.dto.TransferObjectAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectRedeliverResponse;
import com.yss.valset.transfer.application.dto.TransferObjectViewDTO;
import com.yss.valset.transfer.application.service.TransferObjectManagementAppService;
import com.yss.valset.transfer.application.service.TransferObjectQueryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 文件主对象查询接口。
 */
@RestController
@RequestMapping("/transfer-objects")
public class TransferObjectController {

    private final TransferObjectQueryService transferObjectQueryService;
    private final TransferObjectManagementAppService transferObjectManagementAppService;

    public TransferObjectController(TransferObjectQueryService transferObjectQueryService,
                                    TransferObjectManagementAppService transferObjectManagementAppService) {
        this.transferObjectQueryService = transferObjectQueryService;
        this.transferObjectManagementAppService = transferObjectManagementAppService;
    }

    /**
     * 查询文件主对象详情。
     *
     * @param transferId 文件主键
     * @return 文件主对象详情
     */
    @GetMapping("/{transferId}")
    @Operation(summary = "查询文件主对象详情", description = "返回文件主体、邮件信息、文件状态、路由信息和元数据。")
    public SingleResult<TransferObjectViewDTO> getObject(@PathVariable String transferId) {
        return SingleResult.of(transferObjectQueryService.getObject(transferId));
    }

    /**
     * 查询文件主对象邮件信息。
     *
     * @param transferId 文件主键
     * @return 文件主对象邮件信息
     */
    @GetMapping("/{transferId}/mail-info")
    @Operation(summary = "查询文件主对象邮件信息", description = "只返回文件主对象关联的邮件信息。")
    public SingleResult<TransferMailInfoViewDTO> getMailInfo(@PathVariable String transferId) {
        return SingleResult.of(transferObjectQueryService.getMailInfo(transferId));
    }

    /**
     * 下载文件主对象对应的本地临时文件。
     *
     * @param transferId 文件主键
     * @return 文件下载响应
     */
    @GetMapping(value = "/{transferId}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "下载文件主对象", description = "根据分拣对象主键下载其本地临时文件，文件内容来自 localTempPath。")
    public ResponseEntity<Resource> downloadObject(@PathVariable String transferId) {
        TransferObjectDownloadViewDTO downloadView = transferObjectQueryService.downloadObject(transferId);
        Resource resource = new FileSystemResource(downloadView.getFilePath());
        MediaType contentType = resolveContentType(downloadView.getContentType());
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(downloadView.getFileName(), StandardCharsets.UTF_8)
                .build();
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .contentType(contentType);
        Long contentLength = downloadView.getContentLength();
        if (contentLength != null && contentLength >= 0) {
            builder.contentLength(contentLength);
        }
        return builder.body(resource);
    }

    /**
     * 分页查询文件主对象列表。
     *
     * @param sourceId 来源主键
     * @param sourceType 来源类型
     * @param sourceCode 来源编码
     * @param status 文件状态
     * @param deliveryStatus 投递状态
     * @param mailId 邮件唯一标识
     * @param fingerprint 文件指纹
     * @param routeId 路由主键
     * @param tagId 标签主键
     * @param tagCode 标签编码
     * @param tagValue 标签值
     * @param pageIndex 页码
     * @param pageSize 每页条数
     * @return 文件主对象分页结果
     */
    @GetMapping
    @Operation(summary = "分页查询文件主对象列表", description = "支持按来源、邮件、指纹、状态、路由和标签查询，按 pageIndex/pageSize 返回分页结果。")
    public PageResult<TransferObjectViewDTO> pageObjects(@RequestParam(value = "sourceId", required = false) String sourceId,
                                                             @RequestParam(value = "sourceType", required = false) String sourceType,
                                                             @RequestParam(value = "sourceCode", required = false) String sourceCode,
                                                             @RequestParam(value = "status", required = false) String status,
                                                             @RequestParam(value = "deliveryStatus", required = false) String deliveryStatus,
                                                             @RequestParam(value = "mailId", required = false) String mailId,
                                                             @RequestParam(value = "fingerprint", required = false) String fingerprint,
                                                             @RequestParam(value = "routeId", required = false) String routeId,
                                                             @RequestParam(value = "tagId", required = false) String tagId,
                                                             @RequestParam(value = "tagCode", required = false) String tagCode,
                                                             @RequestParam(value = "tagValue", required = false) String tagValue,
                                                             @RequestParam(value = "pageIndex", required = false) Integer pageIndex,
                                                             @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return transferObjectQueryService.pageObjects(sourceId, sourceType, sourceCode, status, deliveryStatus, mailId, fingerprint, routeId, tagId, tagCode, tagValue, pageIndex, pageSize);
    }

    /**
     * 分页查询邮件收件箱列表。
     *
     * @param sourceCode 来源编码
     * @param mailId 邮件唯一标识
     * @param deliveryStatus 投递状态
     * @param pageIndex 页码
     * @param pageSize 每页条数
     * @return 邮件收件箱分页结果
     */
    @GetMapping("/mail-inbox")
    @Operation(summary = "分页查询邮件收件箱列表", description = "按 mailId 去重返回邮件列表，同一封邮件的多个 transferId 作为附件集合返回。")
    public PageResult<TransferObjectViewDTO> pageMailInbox(@RequestParam(value = "sourceCode", required = false) String sourceCode,
                                                           @RequestParam(value = "mailId", required = false) String mailId,
                                                           @RequestParam(value = "deliveryStatus", required = false) String deliveryStatus,
                                                           @RequestParam(value = "pageIndex", required = false) Integer pageIndex,
                                                           @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return transferObjectQueryService.pageMailInbox(sourceCode, mailId, deliveryStatus, pageIndex, pageSize);
    }

    /**
     * 统计分析文件主对象。
     *
     * @param sourceId 来源主键
     * @param sourceType 来源类型
     * @param sourceCode 来源编码
     * @param status 文件状态
     * @param deliveryStatus 投递状态
     * @param mailId 邮件唯一标识
     * @param fingerprint 文件指纹
     * @param routeId 路由主键
     * @param tagId 标签主键
     * @param tagCode 标签编码
     * @param tagValue 标签值
     * @return 文件主对象统计分析结果
     */
    @GetMapping("/analysis")
    @Operation(summary = "统计分析文件主对象", description = "按来源编码分组统计文件状态数量，并支持按文件状态和标签筛选。")
    public SingleResult<TransferObjectAnalysisViewDTO> analyzeObjects(@RequestParam(value = "sourceId", required = false) String sourceId,
                                                                      @RequestParam(value = "sourceType", required = false) String sourceType,
                                                                      @RequestParam(value = "sourceCode", required = false) String sourceCode,
                                                                      @RequestParam(value = "status", required = false) String status,
                                                                      @RequestParam(value = "deliveryStatus", required = false) String deliveryStatus,
                                                                      @RequestParam(value = "mailId", required = false) String mailId,
                                                                      @RequestParam(value = "fingerprint", required = false) String fingerprint,
                                                                      @RequestParam(value = "routeId", required = false) String routeId,
                                                                      @RequestParam(value = "tagId", required = false) String tagId,
                                                                      @RequestParam(value = "tagCode", required = false) String tagCode,
                                                                      @RequestParam(value = "tagValue", required = false) String tagValue) {
        return SingleResult.of(transferObjectQueryService.analyzeObjects(sourceId, sourceType, sourceCode, status, deliveryStatus, mailId, fingerprint, routeId, tagId, tagCode, tagValue));
    }

    /**
     * 统计分析邮件收件箱。
     *
     * @param sourceCode 来源编码
     * @param mailId 邮件唯一标识
     * @param deliveryStatus 投递状态
     * @return 邮件收件箱统计分析结果
     */
    @GetMapping("/mail-inbox/analysis")
    @Operation(summary = "统计分析邮件收件箱", description = "按邮件分组统计收件箱数量、文件夹数量和大小信息。")
    public SingleResult<TransferObjectAnalysisViewDTO> analyzeMailInbox(@RequestParam(value = "sourceCode", required = false) String sourceCode,
                                                                        @RequestParam(value = "mailId", required = false) String mailId,
                                                                        @RequestParam(value = "deliveryStatus", required = false) String deliveryStatus) {
        return SingleResult.of(transferObjectQueryService.analyzeMailInbox(sourceCode, mailId, deliveryStatus));
    }

    /**
     * 重新投递文件主对象。
     *
     * @param command 重新投递命令
     * @return 重新投递结果
     */
    @PostMapping("/redeliver")
    @Operation(summary = "重新投递文件主对象", description = "支持对未投递完成的文件主对象执行重新投递，输入文件主键集合即可。")
    public SingleResult<TransferObjectRedeliverResponse> redeliver(@RequestBody TransferObjectRedeliverCommand command) {
        return SingleResult.of(transferObjectManagementAppService.redeliver(command));
    }

    /**
     * 重新打标文件主对象。
     *
     * @param command 重新打标命令
     * @return 文件主对象重新打标结果
     */
    @PostMapping("/retag")
    @Operation(summary = "重新打标文件主对象", description = "按当前筛选条件重新识别文件主对象并覆盖已有标签。")
    public SingleResult<TransferObjectRetagResponse> retag(@RequestBody TransferObjectRetagCommand command) {
        return SingleResult.of(transferObjectManagementAppService.retag(command));
    }

    private MediaType resolveContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
