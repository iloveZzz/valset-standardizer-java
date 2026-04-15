package com.yss.subjectmatch.extract.repository.gateway.impl;

import com.yss.subjectmatch.domain.gateway.ScheduleGateway;
import com.yss.subjectmatch.domain.model.ScheduleDefinition;
import com.yss.subjectmatch.extract.repository.mapper.ScheduleDefinitionRepository;
import com.yss.subjectmatch.extract.repository.convertor.ScheduleDefinitionConvertor;
import com.yss.subjectmatch.extract.repository.entity.ScheduleDefinitionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * MyBatis 支持的调度网关。
 */
@Repository
@RequiredArgsConstructor
public class ScheduleGatewayImpl implements ScheduleGateway {

    private final ScheduleDefinitionRepository scheduleDefinitionRepository;
    private final ScheduleDefinitionConvertor scheduleDefinitionConvertor;

    /**
     * 按 id 加载计划定义。
     */
    @Override
    public ScheduleDefinition findById(Long scheduleId) {
        ScheduleDefinitionPO po = scheduleDefinitionRepository.selectById(scheduleId);
        if (po == null) {
            throw new IllegalStateException("Schedule not found: " + scheduleId);
        }
        return scheduleDefinitionConvertor.toDomain(po);
    }
}
