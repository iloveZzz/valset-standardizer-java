package com.yss.valset.transfer.scheduler;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferObjectRedeliverBootstrapRunnerTest {

    @Test
    void runShouldFallbackToScheduleIfNotExistsWhenInstanceMissing() {
        SchedulerClient schedulerClient = mock(SchedulerClient.class);
        when(schedulerClient.reschedule(any())).thenThrow(new TaskInstanceNotFoundException("transfer-object-redeliver", "default"));
        TransferObjectRedeliverBootstrapRunner runner = new TransferObjectRedeliverBootstrapRunner(schedulerClient);

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(schedulerClient).scheduleIfNotExists(any());
    }
}
