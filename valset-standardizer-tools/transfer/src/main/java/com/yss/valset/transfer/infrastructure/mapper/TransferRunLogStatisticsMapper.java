package com.yss.valset.transfer.infrastructure.mapper;

import com.yss.valset.transfer.infrastructure.dto.TransferRunLogTrendDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件收发运行日志统计查询器。
 */
public interface TransferRunLogStatisticsMapper {

    List<TransferRunLogTrendDTO> selectDeliverTrend(@Param("startInclusive") LocalDateTime startInclusive,
                                                    @Param("endExclusive") LocalDateTime endExclusive);
}
