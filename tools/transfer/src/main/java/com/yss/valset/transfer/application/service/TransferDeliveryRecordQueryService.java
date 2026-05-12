package com.yss.valset.transfer.application.service;

import com.yss.valset.transfer.application.dto.TransferDeliveryRecordSummaryViewDTO;

/**
 * 文件投递结果查询服务。
 */
public interface TransferDeliveryRecordQueryService {

    /**
     * 统计当天文件投递结果。
     *
     * @return 当天文件投递统计视图
     */
    TransferDeliveryRecordSummaryViewDTO summarizeToday();
}
