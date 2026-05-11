package com.yss.valset.parser.web.controller;

import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.parser.application.dto.ParseQueueObserverRunSummary;
import com.yss.valset.parser.application.port.ParseQueueObservationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 待解析观察者管理接口。
 */
@RestController
@RequestMapping("/parse-queue-observer")
@ConditionalOnProperty(prefix = "valset.features.parse-queue-observer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ParseQueueObserverController {

    private final ParseQueueObservationUseCase parseQueueObservationUseCase;

    public ParseQueueObserverController(ParseQueueObservationUseCase parseQueueObservationUseCase) {
        this.parseQueueObservationUseCase = parseQueueObservationUseCase;
    }

    /**
     * 立即执行一轮待解析事件观察。
     */
    @PostMapping("/run")
    @Operation(summary = "立即执行待解析观察")
    public SingleResult<ParseQueueObserverRunSummary> run() {
        return SingleResult.of(parseQueueObservationUseCase.runObservation());
    }
}
