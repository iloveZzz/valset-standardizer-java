package com.yss.valset.transfer.infrastructure.mapper;

import com.yss.valset.transfer.infrastructure.dto.TransferObjectTrendDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分拣对象趋势查询器。
 */
public interface TransferObjectTrendMapper {

    List<TransferObjectTrendDTO> selectDeliveryTrend(@Param("startInclusive") LocalDateTime startInclusive,
                                                     @Param("endExclusive") LocalDateTime endExclusive);
}
