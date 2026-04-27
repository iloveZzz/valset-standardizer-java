package com.yss.valset.transfer.application.service;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.transfer.application.dto.TransferObjectDownloadViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferMailInfoViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectViewDTO;

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
     * 查询文件主对象邮件信息。
     *
     * @param transferId 文件主键
     * @return 文件主对象邮件信息
     */
    TransferMailInfoViewDTO getMailInfo(String transferId);

    /**
     * 准备文件主对象下载内容。
     *
     * @param transferId 文件主键
     * @return 文件主对象下载信息
     */
    TransferObjectDownloadViewDTO downloadObject(String transferId);

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
    PageResult<TransferObjectViewDTO> pageObjects(String sourceId,
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
    PageResult<TransferObjectViewDTO> pageMailInbox(String sourceCode,
                                                    String mailId,
                                                    String deliveryStatus,
                                                    Integer pageIndex,
                                                    Integer pageSize);

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
                                                 String deliveryStatus,
                                                 String mailId,
                                                 String fingerprint,
                                                 String routeId,
                                                 String tagId,
                                                 String tagCode,
                                                 String tagValue);

    /**
     * 统计分析邮件收件箱。
     *
     * @param sourceCode 来源编码
     * @param mailId 邮件唯一标识
     * @param deliveryStatus 投递状态
     * @return 邮件收件箱统计分析结果
     */
    TransferObjectAnalysisViewDTO analyzeMailInbox(String sourceCode,
                                                   String mailId,
                                                   String deliveryStatus);
}
