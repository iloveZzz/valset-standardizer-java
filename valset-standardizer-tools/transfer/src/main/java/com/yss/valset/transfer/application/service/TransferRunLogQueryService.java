package com.yss.valset.transfer.application.service;

import com.yss.cloud.dto.result.PageResult;
import com.yss.valset.transfer.application.dto.TransferRunLogViewDTO;
import com.yss.valset.transfer.application.dto.TransferRunLogAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferRunLogTrendViewDTO;

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
     * @param taskDate 任务日期
     * @return 文件收发运行日志列表
     */
    List<TransferRunLogViewDTO> listLogs(String sourceId,
                                         String transferId,
                                         String routeId,
                                         String runStage,
                                         String runStatus,
                                         String triggerType,
                                         String taskDate,
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
     * @param taskDate 任务日期
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
                                               String taskDate,
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
     * @param taskDate 任务日期
     * @return 文件收发运行日志统计分析结果
     */
    TransferRunLogAnalysisViewDTO analyzeLogs(String sourceId,
                                              String transferId,
                                              String routeId,
                                              String runStage,
                                              String runStatus,
                                              String triggerType,
                                              String keyword,
                                              String taskDate);

    /**
     * 统计文件投递趋势。
     *
     * @param days 天数
     * @param taskDate 任务日期
     * @return 文件投递趋势
     */
    List<TransferRunLogTrendViewDTO> trendLogs(Integer days, String taskDate);
}
