package com.yss.subjectmatch.domain.gateway;

import com.yss.subjectmatch.domain.model.ScheduleDefinition;

/**
 * 计划定义持久性的网关。
 */
public interface ScheduleGateway {
    /**
     * 按 id 加载计划定义。
     */
    ScheduleDefinition findById(Long scheduleId);
}
