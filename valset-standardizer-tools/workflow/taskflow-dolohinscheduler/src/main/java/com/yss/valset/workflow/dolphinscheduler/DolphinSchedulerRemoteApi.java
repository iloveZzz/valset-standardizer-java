package com.yss.valset.workflow.dolphinscheduler;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * DolphinScheduler 外部平台 Feign 接口。
 */
@FeignClient(
        name = "${valset.workflow.dolphinscheduler.service-name:dolphin-scheduler}",
        url = "${valset.workflow.dolphinscheduler.base-url:}"
)
public interface DolphinSchedulerRemoteApi {

    @GetMapping("/projects")
    String listProjects(@SpringQueryMap Map<String, String> queryParams);

    @GetMapping("/projects/{projectCode}")
    String getProject(@PathVariable("projectCode") Long projectCode);

    @PostMapping(value = "/projects", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String createProject(@RequestBody MultiValueMap<String, String> formParams);

    @GetMapping("/projects/{projectCode}/workflow-definition/{workflowCode}")
    String getWorkflowDefinition(@PathVariable("projectCode") Long projectCode,
                                 @PathVariable("workflowCode") Long workflowCode);

    @PostMapping(value = "/projects/{projectCode}/workflow-definition", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String createWorkflowDefinition(@PathVariable("projectCode") Long projectCode,
                                    @RequestBody MultiValueMap<String, String> formParams);

    @PutMapping(value = "/projects/{projectCode}/workflow-definition/{workflowCode}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String updateWorkflowDefinition(@PathVariable("projectCode") Long projectCode,
                                    @PathVariable("workflowCode") Long workflowCode,
                                    @RequestBody MultiValueMap<String, String> formParams);

    @DeleteMapping("/projects/{projectCode}/workflow-definition/{workflowCode}")
    String deleteWorkflowDefinition(@PathVariable("projectCode") Long projectCode,
                                    @PathVariable("workflowCode") Long workflowCode);

    @PostMapping(value = "/projects/{projectCode}/workflow-definition/{workflowCode}/release", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String releaseWorkflowDefinition(@PathVariable("projectCode") Long projectCode,
                                     @PathVariable("workflowCode") Long workflowCode,
                                     @RequestBody MultiValueMap<String, String> formParams);

    @GetMapping("/projects/{projectCode}/workflow/instances")
    String listWorkflowInstances(@PathVariable("projectCode") Long projectCode,
                                 @SpringQueryMap Map<String, String> queryParams);

    @GetMapping("/projects/{projectCode}/workflow-instances")
    String listWorkflowInstancesLegacy(@PathVariable("projectCode") Long projectCode,
                                       @SpringQueryMap Map<String, String> queryParams);

    @GetMapping("/projects/{projectCode}/workflow-instances/{workflowInstanceId}")
    String getWorkflowInstance(@PathVariable("projectCode") Long projectCode,
                               @PathVariable("workflowInstanceId") String workflowInstanceId);

    @GetMapping("/projects/{projectCode}/workflow-instances/{workflowInstanceId}/tasks")
    String listWorkflowInstanceTasks(@PathVariable("projectCode") Long projectCode,
                                     @PathVariable("workflowInstanceId") String workflowInstanceId);

    @GetMapping("/projects/{projectCode}/task-instances")
    String listTaskInstances(@PathVariable("projectCode") Long projectCode,
                             @SpringQueryMap Map<String, String> queryParams);

    @GetMapping("/projects/analysis/task-state-count")
    String countTaskState(@SpringQueryMap Map<String, String> queryParams);

    @GetMapping("/projects/analysis/workflow-state-count")
    String countWorkflowState(@SpringQueryMap Map<String, String> queryParams);

    @PostMapping("/projects/{projectCode}/task-instances/{id}/force-success")
    String forceTaskSuccess(@PathVariable("projectCode") Long projectCode,
                            @PathVariable("id") Integer id);

    @PostMapping(value = "/projects/{projectCode}/executors/start-workflow-instance", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String startWorkflowInstance(@PathVariable("projectCode") Long projectCode,
                                 @RequestBody MultiValueMap<String, String> formParams);

    @PostMapping(value = "/projects/{projectCode}/executors/execute", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String executeWorkflow(@PathVariable("projectCode") Long projectCode,
                           @RequestBody MultiValueMap<String, String> formParams);

    @GetMapping("/dolphinscheduler/log/detail")
    String getTaskLogLegacy(@SpringQueryMap Map<String, String> queryParams);

    @GetMapping("/log/detail")
    String getTaskLog(@SpringQueryMap Map<String, String> queryParams);

    @GetMapping("/log/{projectCode}/detail")
    String getTaskLogByProject(@PathVariable("projectCode") Long projectCode,
                               @SpringQueryMap Map<String, String> queryParams);

    @PostMapping(value = "/projects/{projectCode}/schedules", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String createSchedule(@PathVariable("projectCode") Long projectCode,
                          @RequestBody MultiValueMap<String, String> formParams);

    @PutMapping(value = "/projects/{projectCode}/schedules/{scheduleId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String updateSchedule(@PathVariable("projectCode") Long projectCode,
                          @PathVariable("scheduleId") Long scheduleId,
                          @RequestBody MultiValueMap<String, String> formParams);

    @PutMapping(value = "/projects/{projectCode}/schedules/update/{workflowDefinitionCode}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String updateScheduleByWorkflowDefinitionCode(@PathVariable("projectCode") Long projectCode,
                                                  @PathVariable("workflowDefinitionCode") Long workflowDefinitionCode,
                                                  @RequestBody MultiValueMap<String, String> formParams);

    @DeleteMapping("/projects/{projectCode}/schedules/{scheduleId}")
    String deleteSchedule(@PathVariable("projectCode") Long projectCode,
                          @PathVariable("scheduleId") Long scheduleId);

    @PostMapping(value = "/projects/{projectCode}/schedules/{scheduleId}/online", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String onlineSchedule(@PathVariable("projectCode") Long projectCode,
                          @PathVariable("scheduleId") Long scheduleId);

    @PostMapping(value = "/projects/{projectCode}/schedules/{scheduleId}/offline", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String offlineSchedule(@PathVariable("projectCode") Long projectCode,
                           @PathVariable("scheduleId") Long scheduleId);

    @GetMapping("/projects/{projectCode}/schedules")
    String listSchedules(@PathVariable("projectCode") Long projectCode,
                         @SpringQueryMap Map<String, String> queryParams);

    @PostMapping(value = "/projects/{projectCode}/schedules/preview", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String previewSchedule(@PathVariable("projectCode") Long projectCode,
                           @RequestBody MultiValueMap<String, String> formParams);
}
