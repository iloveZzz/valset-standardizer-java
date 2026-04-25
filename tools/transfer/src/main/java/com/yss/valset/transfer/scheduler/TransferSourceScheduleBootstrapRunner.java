package com.yss.valset.transfer.scheduler;

import com.yss.valset.transfer.application.impl.management.TransferSourceScheduleCoordinator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 应用启动后的文件来源调度对账任务。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class TransferSourceScheduleBootstrapRunner implements ApplicationRunner {

    private final TransferSourceScheduleCoordinator transferSourceScheduleCoordinator;

    @Override
    public void run(ApplicationArguments args) {
        transferSourceScheduleCoordinator.reconcileAllSourcesOnStartup();
    }
}
