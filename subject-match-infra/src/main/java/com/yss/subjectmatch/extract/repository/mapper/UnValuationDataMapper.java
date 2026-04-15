package com.yss.subjectmatch.extract.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yss.subjectmatch.extract.repository.entity.ValuationDataPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 访问未标准化的估值表数据
 */
@Mapper
public interface UnValuationDataMapper extends BaseMapper<ValuationDataPO> {
}
