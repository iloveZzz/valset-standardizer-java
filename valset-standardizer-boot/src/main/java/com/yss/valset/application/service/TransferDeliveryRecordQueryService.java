package com.yss.valset.application.service;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.application.dto.TransferDeliveryRecordViewDTO;

import java.util.List;

/**
 * 文件投递结果查询服务。
 */
public interface TransferDeliveryRecordQueryService {

    /**
     * 查询文件投递结果列表。
     *
     * @param routeId 路由主键
     * @param transferId 文件主键
     * @param targetCode 目标编码
     * @param executeStatus 执行状态
     * @param limit 查询上限
     * @return 文件投递结果列表
     */
    List<TransferDeliveryRecordViewDTO> listRecords(String routeId, String transferId, String targetCode, String executeStatus, Integer limit);

    /**
     * 分页查询文件投递结果列表。
     *
     * @param routeId 路由主键
     * @param transferId 文件主键
     * @param targetCode 目标编码
     * @param executeStatus 执行状态
     * @param pageIndex 页码
     * @param pageSize 每页条数
     * @return 文件投递结果分页结果
     */
    PageResult<TransferDeliveryRecordViewDTO> pageRecords(String routeId, String transferId, String targetCode, String executeStatus, Integer pageIndex, Integer pageSize);

    /**
     * 查询文件投递结果详情。
     *
     * @param deliveryId 投递记录主键
     * @return 文件投递结果详情
     */
    TransferDeliveryRecordViewDTO getRecord(String deliveryId);
}
