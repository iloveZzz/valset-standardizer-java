package com.yss.valset.transfer.application.service;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.transfer.application.dto.TransferObjectViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectPageViewDTO;

import java.util.List;

/**
 * 文件主对象查询服务。
 */
public interface TransferObjectQueryService {

    /**
     * 查询文件主对象详情。
     *
     * @param transferId 文件主键
     * @return 文件主对象详情
     */
    TransferObjectViewDTO getObject(String transferId);

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
     * @param pageIndex 页码
     * @param pageSize 每页条数
     * @return 文件主对象分页结果
     */
    PageResult<TransferObjectViewDTO> pageObjects(String sourceId, String sourceType, String sourceCode, String status, String mailId, String fingerprint, String routeId, Integer pageIndex, Integer pageSize);

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
     * @return 文件主对象统计分析结果
     */
    TransferObjectAnalysisViewDTO analyzeObjects(String sourceId,
                                                 String sourceType,
                                                 String sourceCode,
                                                 String status,
                                                 String mailId,
                                                 String fingerprint,
                                                 String routeId);
}
