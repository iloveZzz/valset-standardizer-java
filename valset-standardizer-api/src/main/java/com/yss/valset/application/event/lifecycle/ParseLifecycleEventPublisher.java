package com.yss.valset.application.event.lifecycle;

/**
 * 解析生命周期事件发布器。
 */
public interface ParseLifecycleEventPublisher {

    void publish(ParseLifecycleEvent event);
}
