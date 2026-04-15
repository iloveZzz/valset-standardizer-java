package com.yss.subjectmatch.extract.repository.mapper;

import com.yss.cloud.mybatis.support.BasePlusRepository;
import com.yss.subjectmatch.extract.repository.entity.ValuationFileDataPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 估值原始行数据 ODS 表的 MyBatis Mapper。
 * 提供原始提数结果的批量落库和查询能力。
 */
@Mapper
public interface ValuationFileDataMapper extends BasePlusRepository<ValuationFileDataPO> {

    /**
     * 查询指定任务下的全部原始行数据。
     *
     * @param taskId 任务标识
     * @return 对应任务的原始行数据列表
     */
    List<ValuationFileDataPO> findByTaskId(@Param("taskId") Long taskId);

    /**
     * 查询指定文件下的全部原始行数据。
     *
     * @param fileId 文件标识
     * @return 对应文件的原始行数据列表
     */
    List<ValuationFileDataPO> findByFileId(@Param("fileId") Long fileId);
}
