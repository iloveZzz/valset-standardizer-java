package com.yss.valset.extract.repository.mapper;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.valset.extract.repository.entity.ValuationSheetStylePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 估值表 sheet 样式快照 ODS 表的 MyBatis Mapper。
 */
@Mapper
public interface ValuationSheetStyleMapper extends BasePlusRepository<ValuationSheetStylePO> {

    /**
     * 查询指定任务下的 sheet 样式快照。
     *
     * @param taskId 任务标识
     * @return sheet 样式快照列表
     */
    List<ValuationSheetStylePO> findByTaskId(@Param("taskId") Long taskId);

    /**
     * 查询指定文件下的 sheet 样式快照。
     *
     * @param fileId 文件标识
     * @return sheet 样式快照列表
     */
    List<ValuationSheetStylePO> findByFileId(@Param("fileId") Long fileId);
}
