package com.yss.valset.controller;

import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.batch.job.ParseQueueObserverJob;
import com.yss.valset.batch.job.ParseQueueObserverRunSummary;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 待解析观察者管理接口。
 */
@RestController
@RequestMapping("/parse-queue-observer")
public class ParseQueueObserverController {

    private final ParseQueueObserverJob parseQueueObserverJob;

    public ParseQueueObserverController(ParseQueueObserverJob parseQueueObserverJob) {
        this.parseQueueObserverJob = parseQueueObserverJob;
    }

    /**
     * 立即执行一轮待解析事件观察。
     */
    @PostMapping("/run")
    @Operation(summary = "立即执行待解析观察")
    public SingleResult<ParseQueueObserverRunSummary> run() {
        return SingleResult.of(parseQueueObserverJob.runObservation());
    }
}
