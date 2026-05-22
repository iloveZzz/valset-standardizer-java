package com.yss.valset.task.application.service;

import com.yss.valset.task.application.command.OutsourcedDataTaskStandardDataExportCommand;
import com.yss.valset.task.application.dto.OutsourcedDataTaskExternalMetricDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskExternalSubjectDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskRawWorkbookDownloadDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskRawWorkbookDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStandardBasicDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStandardDataExportDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStandardMetricDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStandardSubjectDTO;

import java.util.List;

/**
 * 估值解析批次标准数据查询服务。
 */
public interface OutsourcedDataTaskStandardDataService {

    OutsourcedDataTaskStandardBasicDTO queryBasic(String batchId);

    List<OutsourcedDataTaskStandardSubjectDTO> listSubjects(String batchId, String keyword);

    List<OutsourcedDataTaskStandardMetricDTO> listMetrics(String batchId, String keyword);

    List<OutsourcedDataTaskExternalSubjectDTO> listExternalSubjects(String batchId, String keyword);

    List<OutsourcedDataTaskExternalMetricDTO> listExternalMetrics(String batchId, String keyword);

    OutsourcedDataTaskRawWorkbookDTO queryRawWorkbook(String batchId);

    OutsourcedDataTaskRawWorkbookDownloadDTO downloadRawWorkbook(String batchId);

    OutsourcedDataTaskStandardDataExportDTO exportSheet(String batchId, OutsourcedDataTaskStandardDataExportCommand command);
}
