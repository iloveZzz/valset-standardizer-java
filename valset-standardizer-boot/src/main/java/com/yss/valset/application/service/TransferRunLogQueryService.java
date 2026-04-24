package com.yss.valset.application.service;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.application.dto.TransferRunLogViewDTO;
import com.yss.valset.application.dto.TransferRunLogAnalysisViewDTO;

import java.util.List;

/**
 * 文件收发运行日志查询服务。
 */
public interface TransferRunLogQueryService {

    /**
     * 查询文件收发运行日志列表。
     *
     * @param sourceId 来源主键
     * @param transferId 文件主键
     * @param routeId 路由主键
     * @param runStage 运行阶段
     * @param runStatus 运行状态
     * @param triggerType 触发类型
     * @param limit 查询上限
     * @return 文件收发运行日志列表
     */
    List<TransferRunLogViewDTO> listLogs(String sourceId,
                                         String transferId,
                                         String routeId,
                                         String runStage,
                                         String runStatus,
                                         String triggerType,
                                         Integer limit);

    /**
     * 分页查询文件收发运行日志。
     *
     * @param sourceId 来源主键
     * @param transferId 文件主键
     * @param routeId 路由主键
     * @param runStage 运行阶段
     * @param runStatus 运行状态
     * @param triggerType 触发类型
     * @param keyword 关键字
     * @param pageIndex 页码
     * @param pageSize 每页条数
     * @return 文件收发运行日志分页结果
     */
    PageResult<TransferRunLogViewDTO> pageLogs(String sourceId,
                                               String transferId,
                                               String routeId,
                                               String runStage,
                                               String runStatus,
                                               String triggerType,
                                               String keyword,
                                               Integer pageIndex,
                                               Integer pageSize);

    /**
     * 统计分析文件收发运行日志。
     *
     * @param sourceId 来源主键
     * @param transferId 文件主键
     * @param routeId 路由主键
     * @param runStage 运行阶段
     * @param runStatus 运行状态
     * @param triggerType 触发类型
     * @param keyword 关键字
     * @return 文件收发运行日志统计分析结果
     */
    TransferRunLogAnalysisViewDTO analyzeLogs(String sourceId,
                                              String transferId,
                                              String routeId,
                                              String runStage,
                                              String runStatus,
                                              String triggerType,
                                              String keyword);
}
