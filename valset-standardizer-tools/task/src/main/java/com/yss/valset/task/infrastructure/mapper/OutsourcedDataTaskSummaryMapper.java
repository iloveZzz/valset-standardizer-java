package com.yss.valset.task.infrastructure.mapper;

import com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStageSummaryDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskSummaryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 估值表解析任务统计查询。
 */
public interface OutsourcedDataTaskSummaryMapper {

    OutsourcedDataTaskSummaryDTO selectSummary(@Param("query") OutsourcedDataTaskQueryCommand query);

    List<OutsourcedDataTaskStageSummaryDTO> selectStageSummaries(@Param("query") OutsourcedDataTaskQueryCommand query);

    Long countPageTasks(@Param("query") OutsourcedDataTaskQueryCommand query);

    List<OutsourcedDataTaskBatchDTO> selectPageTasks(@Param("query") OutsourcedDataTaskQueryCommand query,
            @Param("offset") long offset,
            @Param("limit") int limit);
}
