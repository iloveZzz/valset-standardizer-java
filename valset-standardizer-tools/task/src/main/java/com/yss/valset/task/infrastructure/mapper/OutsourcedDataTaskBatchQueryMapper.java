package com.yss.valset.task.infrastructure.mapper;

import com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStageSummaryDTO;
import com.yss.valset.task.infrastructure.dto.OutsourcedDataTaskTraceRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OutsourcedDataTaskBatchQueryMapper {

    long countTasks(@Param("query") OutsourcedDataTaskQueryCommand query);

    long countStatusTasks(@Param("query") OutsourcedDataTaskQueryCommand query,
            @Param("statuses") String... statuses);

    List<OutsourcedDataTaskBatchDTO> pageTasks(@Param("query") OutsourcedDataTaskQueryCommand query,
            @Param("offset") int offset,
            @Param("limit") int limit);

    List<OutsourcedDataTaskStageSummaryDTO> queryStageSummaries(@Param("query") OutsourcedDataTaskQueryCommand query);

    OutsourcedDataTaskBatchDTO findTaskByBatchId(@Param("batchId") String batchId);

    Long findExecutionIdByBatchId(@Param("batchId") String batchId);

    OutsourcedDataTaskTraceRow findTraceRowByBatchId(@Param("batchId") String batchId);
}
