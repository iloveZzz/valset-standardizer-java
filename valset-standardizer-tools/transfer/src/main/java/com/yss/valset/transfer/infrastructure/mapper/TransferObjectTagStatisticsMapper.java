package com.yss.valset.transfer.infrastructure.mapper;

import com.yss.valset.transfer.infrastructure.dto.TransferObjectTagSummaryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 标签识别结果统计查询器。
 */
public interface TransferObjectTagStatisticsMapper {

    List<TransferObjectTagSummaryDTO> selectTagSummary(@Param("startInclusive") LocalDateTime startInclusive,
                                                       @Param("endExclusive") LocalDateTime endExclusive);
}
