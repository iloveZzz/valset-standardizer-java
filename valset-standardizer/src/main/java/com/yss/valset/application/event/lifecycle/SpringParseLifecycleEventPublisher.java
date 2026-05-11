package com.yss.valset.application.event.lifecycle;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 基于 Spring 的解析生命周期事件发布器。
 */
@Component
public class SpringParseLifecycleEventPublisher implements ParseLifecycleEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringParseLifecycleEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(ParseLifecycleEvent event) {
        if (event != null) {
            applicationEventPublisher.publishEvent(event);
        }
    }
}
