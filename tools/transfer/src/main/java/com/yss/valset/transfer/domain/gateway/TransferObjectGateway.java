package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.application.impl.query.DefaultTransferObjectQueryService;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectAnalysis;
import com.yss.valset.transfer.domain.model.TransferObjectPage;

import java.util.List;
import java.util.Optional;

/**
 * 文件主对象网关。
 */
public interface TransferObjectGateway {

    Optional<TransferObject> findById(String transferId);

    Optional<TransferObject> findByFingerprint(String fingerprint);

    TransferObjectPage pageObjects(String sourceId,
                                   String sourceType,
                                   String sourceCode,
                                   String status,
                                   String deliveryStatus,
                                   String mailId,
                                   String fingerprint,
                                   String routeId,
                                   String tagId,
                                   String tagCode,
                                   String tagValue,
                                   Integer pageIndex,
                                   Integer pageSize);

    TransferObjectAnalysis analyzeObjects(String sourceId,
                                          String sourceType,
                                          String sourceCode,
                                          String status,
                                          String deliveryStatus,
                                          String mailId,
                                          String fingerprint,
                                          String routeId,
                                          String tagId,
                                          String tagCode,
                                          String tagValue);

    List<TransferObject> listEmailInboxObjects(String sourceCode, String mailId);

    List<TransferObject> listParseQueueCandidates(String sourceId,
                                                  String sourceCode,
                                                  String routeId,
                                                  String status,
                                                  String deliveryStatus,
                                                  Integer limit);
    
    /**
     * 加载邮件收件箱分组（支持分页）。
     *
     * @param sourceCode 来源编码
     * @param mailId 邮件ID
     * @param deliveryStatus 投递状态
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 邮件分组列表
     */
    List<DefaultTransferObjectQueryService.InboxMailGroup> loadMailInboxGroups(String sourceCode, String mailId, String deliveryStatus, Integer offset, Integer limit);
    
    /**
     * 统计邮件收件箱分组总数。
     *
     * @param sourceCode 来源编码
     * @param mailId 邮件ID
     * @param deliveryStatus 投递状态
     * @return 分组总数
     */
    long countMailInboxGroups(String sourceCode, String mailId, String deliveryStatus);
    
    TransferObject save(TransferObject transferObject);
}
