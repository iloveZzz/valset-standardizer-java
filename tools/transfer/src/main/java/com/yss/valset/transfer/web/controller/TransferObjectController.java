package com.yss.valset.transfer.web.controller;

import com.yss.cloud.dto.response.PageResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.transfer.application.dto.TransferObjectAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectPageViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectViewDTO;
import com.yss.valset.transfer.application.service.TransferObjectQueryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件主对象查询接口。
 */
@RestController
@RequestMapping("/api/transfer-objects")
public class TransferObjectController {

    private final TransferObjectQueryService transferObjectQueryService;

    public TransferObjectController(TransferObjectQueryService transferObjectQueryService) {
        this.transferObjectQueryService = transferObjectQueryService;
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
     * 分页查询文件主对象列表。
     *
     * @param sourceId 来源主键
     * @param sourceType 来源类型
     * @param sourceCode 来源编码
     * @param status 文件状态
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
                                                             @RequestParam(value = "mailId", required = false) String mailId,
                                                             @RequestParam(value = "fingerprint", required = false) String fingerprint,
                                                             @RequestParam(value = "routeId", required = false) String routeId,
                                                             @RequestParam(value = "tagId", required = false) String tagId,
                                                             @RequestParam(value = "tagCode", required = false) String tagCode,
                                                             @RequestParam(value = "tagValue", required = false) String tagValue,
                                                             @RequestParam(value = "pageIndex", required = false) Integer pageIndex,
                                                             @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return transferObjectQueryService.pageObjects(sourceId, sourceType, sourceCode, status, mailId, fingerprint, routeId, tagId, tagCode, tagValue, pageIndex, pageSize);
    }

    /**
     * 统计分析文件主对象。
     *
     * @param sourceId 来源主键
     * @param sourceType 来源类型
     * @param sourceCode 来源编码
     * @param status 文件状态
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
                                                                      @RequestParam(value = "mailId", required = false) String mailId,
                                                                      @RequestParam(value = "fingerprint", required = false) String fingerprint,
                                                                      @RequestParam(value = "routeId", required = false) String routeId,
                                                                      @RequestParam(value = "tagId", required = false) String tagId,
                                                                      @RequestParam(value = "tagCode", required = false) String tagCode,
                                                                      @RequestParam(value = "tagValue", required = false) String tagValue) {
        return SingleResult.of(transferObjectQueryService.analyzeObjects(sourceId, sourceType, sourceCode, status, mailId, fingerprint, routeId, tagId, tagCode, tagValue));
    }
}
