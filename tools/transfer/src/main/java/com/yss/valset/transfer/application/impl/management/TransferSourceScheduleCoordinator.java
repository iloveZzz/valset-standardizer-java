package com.yss.valset.transfer.application.impl.management;

import com.yss.valset.transfer.application.port.TransferJobScheduler;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.TransferSource;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文件来源轮询调度对账协调器。
 */
@Service
@RequiredArgsConstructor
public class TransferSourceScheduleCoordinator {

    private static final Logger log = LoggerFactory.getLogger(TransferSourceScheduleCoordinator.class);

    private final TransferSourceGateway transferSourceGateway;
    private final TransferJobScheduler transferJobScheduler;

    public void reconcileAllSourcesOnStartup() {
        List<TransferSource> sources = transferSourceGateway.listSources(null, null, null, null, null);
        log.info("开始对账文件来源轮询任务，sourceCount={}", sources.size());
        for (TransferSource source : sources) {
            try {
                syncSourceSchedule(source);
            } catch (RuntimeException exception) {
                log.error("文件来源轮询任务对账失败，sourceId={}，sourceCode={}，enabled={}，pollCron={}，message={}",
                        source == null ? null : source.sourceId(),
                        source == null ? null : source.sourceCode(),
                        source != null && source.enabled(),
                        source == null ? null : source.pollCron(),
                        exception.getMessage(),
                        exception);
            }
        }
        log.info("完成文件来源轮询任务对账");
    }

    public void syncSourceSchedule(TransferSource source) {
        if (source == null || source.sourceId() == null) {
            return;
        }
        if (!source.enabled() || source.pollCron() == null || source.pollCron().isBlank()) {
            transferJobScheduler.unscheduleIngest(source.sourceId());
            return;
        }
        transferJobScheduler.scheduleIngestCron(
                source.sourceId(),
                source.sourceType() == null ? null : source.sourceType().name(),
                source.sourceCode(),
                source.connectionConfig(),
                source.pollCron()
        );
    }
}
