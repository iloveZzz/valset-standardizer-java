package com.yss.valset.analysis.application.listener;

import com.yss.valset.application.event.lifecycle.ParseLifecycleEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 解析生命周期事件审计监听器。
 */
@Slf4j
@Component
public class ParseLifecycleEventAuditListener {

    @EventListener
    public void onParseLifecycleEvent(ParseLifecycleEvent event) {
        if (event == null) {
            return;
        }
        log.debug("解析生命周期事件，stage={}, source={}, queueId={}, transferId={}, taskId={}, message={}",
                event.getStage(),
                event.getSource(),
                event.getQueueId(),
                event.getTransferId(),
                event.getTaskId(),
                event.getMessage());
    }
}
