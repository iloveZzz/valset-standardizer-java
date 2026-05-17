package com.yss.valset.batch.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 批处理执行上下文维护 Mapper。
 */
@Mapper
public interface BatchExecutionContextMaintenanceMapper {

    /**
     * 删除指定时间点之前已经结束的作业执行上下文。
     *
     * @param cleanupBefore 清理截止时间
     * @return 删除行数
     */
    int deleteJobExecutionContextsBefore(@Param("cleanupBefore") LocalDateTime cleanupBefore);

    /**
     * 删除指定时间点之前已经结束的步骤执行上下文。
     *
     * @param cleanupBefore 清理截止时间
     * @return 删除行数
     */
    int deleteStepExecutionContextsBefore(@Param("cleanupBefore") LocalDateTime cleanupBefore);
}
