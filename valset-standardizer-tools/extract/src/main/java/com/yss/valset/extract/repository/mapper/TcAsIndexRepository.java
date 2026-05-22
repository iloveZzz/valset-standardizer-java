package com.yss.valset.extract.repository.mapper;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.valset.extract.repository.entity.TcAsIndexPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标准资产指标表仓储。
 */
@Mapper
public interface TcAsIndexRepository extends BasePlusRepository<TcAsIndexPO> {
}
