package com.yss.valset.transfer.infrastructure.convertor;

import com.yss.valset.transfer.domain.model.TransferRunLog;
import com.yss.valset.transfer.infrastructure.entity.TransferRunLogPO;
import org.mapstruct.Mapper;

/**
 * 文件收发运行日志映射器。
 */
@Mapper(componentModel = "spring")
public interface TransferRunLogMapper extends TransferMapstructSupport {

    default TransferRunLogPO toPO(TransferRunLog transferRunLog) {
        if (transferRunLog == null) {
            return null;
        }
        TransferRunLogPO po = new TransferRunLogPO();
        po.setRunLogId(transferRunLog.runLogId());
        po.setSourceId(transferRunLog.sourceId());
        po.setSourceType(transferRunLog.sourceType());
        po.setSourceCode(transferRunLog.sourceCode());
        po.setSourceName(transferRunLog.sourceName());
        po.setTransferId(transferRunLog.transferId());
        po.setRouteId(transferRunLog.routeId());
        po.setTriggerType(transferRunLog.triggerType());
        po.setRunStage(transferRunLog.runStage());
        po.setRunStatus(transferRunLog.runStatus());
        po.setLogMessage(transferRunLog.logMessage());
        po.setErrorMessage(transferRunLog.errorMessage());
        po.setCreatedAt(transferRunLog.createdAt());
        return po;
    }

    default TransferRunLog toDomain(TransferRunLogPO po) {
        if (po == null) {
            return null;
        }
        return new TransferRunLog(
                po.getRunLogId(),
                po.getSourceId(),
                po.getSourceType(),
                po.getSourceCode(),
                po.getSourceName(),
                po.getTransferId(),
                po.getRouteId(),
                po.getTriggerType(),
                po.getRunStage(),
                po.getRunStatus(),
                po.getLogMessage(),
                po.getErrorMessage(),
                po.getCreatedAt()
        );
    }
}
