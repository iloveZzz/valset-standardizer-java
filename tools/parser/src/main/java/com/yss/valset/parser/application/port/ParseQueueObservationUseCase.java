package com.yss.valset.parser.application.port;

import com.yss.valset.parser.application.dto.ParseQueueObserverRunSummary;

/**
 * 待解析观察执行用例。
 */
public interface ParseQueueObservationUseCase {

    /**
     * 立即执行一轮待解析事件观察。
     *
     * @return 观察执行摘要
     */
    ParseQueueObserverRunSummary runObservation();
}
