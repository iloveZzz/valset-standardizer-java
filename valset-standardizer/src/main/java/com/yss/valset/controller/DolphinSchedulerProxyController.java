package com.yss.valset.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.yss.valset.workflow.dolphinscheduler.DolphinSchedulerResponseSupport;
import com.yss.valset.workflow.dolphinscheduler.DolphinSchedulerRemoteApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DolphinScheduler 代理接口。
 */
@RestController
@RequiredArgsConstructor
public class DolphinSchedulerProxyController {

    private final DolphinSchedulerResponseSupport responseSupport;
    private final DolphinSchedulerRemoteApi remoteApi;

    @GetMapping("/dolphinscheduler/projects/analysis/task-state-count")
    public JsonNode countTaskState(@RequestParam(value = "startDate", required = false) String startDate,
                                   @RequestParam(value = "endDate", required = false) String endDate,
                                   @RequestParam(value = "projectCode", required = false) Long projectCode) {
        return responseSupport.toJsonNode(remoteApi.countTaskState(buildAnalysisParams(startDate, endDate, projectCode)));
    }

    @GetMapping("/dolphinscheduler/projects/analysis/workflow-state-count")
    public JsonNode countWorkflowState(@RequestParam(value = "startDate", required = false) String startDate,
                                       @RequestParam(value = "endDate", required = false) String endDate,
                                       @RequestParam(value = "projectCode", required = false) Long projectCode) {
        return responseSupport.toJsonNode(remoteApi.countWorkflowState(buildAnalysisParams(startDate, endDate, projectCode)));
    }

    private Map<String, String> buildAnalysisParams(String startDate, String endDate, Long projectCode) {
        Map<String, String> params = new LinkedHashMap<>();
        if (startDate != null) {
            params.put("startDate", startDate);
        }
        if (endDate != null) {
            params.put("endDate", endDate);
        }
        if (projectCode != null) {
            params.put("projectCode", String.valueOf(projectCode));
        }
        return params;
    }
}
