package com.yss.valset.workflow.dolphinscheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * DolphinScheduler 工作流同步支持器。
 */
@RequiredArgsConstructor
public class DolphinSchedulerWorkflowSyncSupport {

    private static final String SYNC_STATE_KEY = "dolphinschedulerSync";
    private static final String PROJECT_CODE_KEY = "projectCode";
    private static final String PROJECT_NAME_KEY = "projectName";
    private static final String NAMESPACE_KEY = "namespace";
    private static final String PARENT_STATE_KEY = "parent";
    private static final String STAGES_STATE_KEY = "stages";
    private static final String WORKFLOW_CODE_KEY = "workflowDefinitionCode";
    private static final String WORKFLOW_VERSION_KEY = "workflowDefinitionVersion";
    private static final String FINGERPRINT_KEY = "fingerprint";
    private static final String TASK_CODES_KEY = "taskCodes";

    private final DolphinSchedulerApiSupport apiSupport;

    public WorkflowDefinitionDTO syncDefinition(WorkflowDefinitionDTO definition) {
        if (definition == null) {
            throw new IllegalArgumentException("工作流定义不能为空");
        }
        if (definition.getEngineBinding() == null) {
            throw new IllegalArgumentException("工作流绑定信息不能为空");
        }
        if (definition.getEngineBinding().getPlatformType() != EtlPlatformType.DOLPHIN_SCHEDULER) {
            throw new IllegalArgumentException("仅支持 DolphinScheduler 工作流同步");
        }
        if (!StringUtils.hasText(definition.getWorkflowCode())
                || !StringUtils.hasText(definition.getWorkflowName())
                || definition.getWorkflowVersionNo() == null) {
            throw new IllegalArgumentException("工作流基本信息不完整");
        }

        List<WorkflowStageDTO> stages = orderedStages(definition);
        if (stages.isEmpty()) {
            throw new IllegalArgumentException("工作流阶段不能为空");
        }

        WorkflowEngineBindingDTO binding = definition.getEngineBinding();
        Map<String, Object> attributes = new LinkedHashMap<>(
                binding.getAttributes() == null ? Map.of() : binding.getAttributes());
        Map<String, Object> syncState = asMap(attributes.get(SYNC_STATE_KEY));
        if (syncState == null) {
            syncState = new LinkedHashMap<>();
        }

        String projectName = normalizeProjectName(definition);
        long projectCode = resolveProjectCode(definition, syncState, projectName);
        String namespace = resolveNamespace(definition, syncState);
        Map<String, Object> parentState = readParentState(syncState);
        Map<String, Map<String, Object>> stageStates = readStageStates(syncState);
        Map<String, Long> childWorkflowCodes = new LinkedHashMap<>();
        for (WorkflowStageDTO stage : stages) {
            Map<String, Object> stageState = stageStates.get(stage.getStageCode());
            RemoteWorkflowDefinition child = syncChildWorkflow(definition, stage, projectCode, stageState);
            childWorkflowCodes.put(stage.getStageCode(), child.workflowCode);
            stageStates.put(stage.getStageCode(), child.toStateMap());
        }

        RemoteWorkflowDefinition parent = syncParentWorkflow(definition,
                projectCode,
                namespace,
                childWorkflowCodes,
                parentState);

        Map<String, Object> syncSnapshot = new LinkedHashMap<>();
        syncSnapshot.put(PROJECT_CODE_KEY, String.valueOf(projectCode));
        syncSnapshot.put(PROJECT_NAME_KEY, projectName);
        syncSnapshot.put(NAMESPACE_KEY, namespace);
        syncSnapshot.put(PARENT_STATE_KEY, parent.toStateMap());
        syncSnapshot.put(STAGES_STATE_KEY, stageStates);

        attributes.put(SYNC_STATE_KEY, syncSnapshot);
        WorkflowEngineBindingDTO updatedBinding = binding.toBuilder()
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .externalProjectCode(String.valueOf(projectCode))
                .externalWorkflowId(String.valueOf(parent.workflowCode))
                .externalNamespace(namespace)
                .remoteWorkflowVersionNo(parent.workflowVersionNo)
                .attributes(attributes)
                .build();
        return definition.toBuilder()
                .engineBinding(updatedBinding)
                .build();
    }

    public WorkflowDefinitionDTO onlineDefinition(WorkflowDefinitionDTO definition) {
        WorkflowDefinitionDTO normalized = requireSyncedDefinition(definition);
        WorkflowEngineBindingDTO binding = normalized.getEngineBinding();
        long projectCode = resolveProjectCodeForOnlineOffline(normalized);
        Map<String, Object> syncState = readSyncState(normalized);
        Map<String, Map<String, Object>> stageStates = readStageStates(syncState);
        for (Map.Entry<String, Map<String, Object>> entry : stageStates.entrySet()) {
            Long childWorkflowCode = asLong(entry.getValue().get(WORKFLOW_CODE_KEY));
            WorkflowStageDTO stage = findStageByCode(normalized, entry.getKey());
            if (childWorkflowCode != null && stage != null) {
                onlineWorkflowDefinition(projectCode, childWorkflowCode, buildChildWorkflowName(normalized, stage));
            }
        }
        Long parentWorkflowCode = asLong(syncState == null ? null : readStateValue(syncState, PARENT_STATE_KEY, WORKFLOW_CODE_KEY));
        if (parentWorkflowCode == null) {
            parentWorkflowCode = asLong(binding.getExternalWorkflowId());
        }
        if (parentWorkflowCode == null) {
            throw new IllegalStateException("DolphinScheduler 工作流未同步");
        }
        onlineWorkflowDefinition(projectCode, parentWorkflowCode, normalized.getWorkflowName());
        return normalized;
    }

    public WorkflowDefinitionDTO offlineDefinition(WorkflowDefinitionDTO definition) {
        WorkflowDefinitionDTO normalized = requireSyncedDefinition(definition);
        WorkflowEngineBindingDTO binding = normalized.getEngineBinding();
        long projectCode = resolveProjectCodeForOnlineOffline(normalized);
        Map<String, Object> syncState = readSyncState(normalized);
        Long parentWorkflowCode = asLong(syncState == null ? null : readStateValue(syncState, PARENT_STATE_KEY, WORKFLOW_CODE_KEY));
        if (parentWorkflowCode == null) {
            parentWorkflowCode = asLong(binding.getExternalWorkflowId());
        }
        if (parentWorkflowCode == null) {
            throw new IllegalStateException("DolphinScheduler 工作流未同步");
        }
        offlineWorkflowDefinition(projectCode, parentWorkflowCode, normalized.getWorkflowName());
        Map<String, Map<String, Object>> stageStates = readStageStates(syncState);
        for (Map.Entry<String, Map<String, Object>> entry : stageStates.entrySet()) {
            Long childWorkflowCode = asLong(entry.getValue().get(WORKFLOW_CODE_KEY));
            WorkflowStageDTO stage = findStageByCode(normalized, entry.getKey());
            if (childWorkflowCode != null && stage != null) {
                offlineWorkflowDefinition(projectCode, childWorkflowCode, buildChildWorkflowName(normalized, stage));
            }
        }
        return normalized;
    }

    public void deleteDefinition(WorkflowDefinitionDTO definition) {
        if (definition == null || definition.getEngineBinding() == null) {
            return;
        }
        Map<String, Object> attributes = definition.getEngineBinding().getAttributes() == null
                ? Map.of()
                : definition.getEngineBinding().getAttributes();
        Map<String, Object> syncState = asMap(attributes.get(SYNC_STATE_KEY));
        long projectCode = resolveProjectCodeForDelete(definition, syncState);
        List<Long> childWorkflowCodes = collectChildWorkflowCodes(syncState);
        for (Long childWorkflowCode : childWorkflowCodes) {
            deleteWorkflowDefinition(projectCode, childWorkflowCode);
        }
        Long parentWorkflowCode = resolveWorkflowCodeForDelete(definition, syncState);
        if (parentWorkflowCode != null) {
            deleteWorkflowDefinition(projectCode, parentWorkflowCode);
        }
    }

    private long resolveProjectCode(WorkflowDefinitionDTO definition, Map<String, Object> syncState) {
        String projectName = normalizeProjectName(definition);
        return resolveProjectCode(definition, syncState, projectName);
    }

    private long resolveProjectCode(WorkflowDefinitionDTO definition,
                                    Map<String, Object> syncState,
                                    String projectName) {
        Set<Long> candidates = new HashSet<>();
        Long syncProjectCode = asLong(syncState.get(PROJECT_CODE_KEY));
        WorkflowEngineBindingDTO binding = definition.getEngineBinding();
        Long bindingProjectCode = binding == null ? null : asLong(binding.getExternalProjectCode());
        if (syncProjectCode != null) {
            candidates.add(syncProjectCode);
        }
        if (bindingProjectCode != null) {
            candidates.add(bindingProjectCode);
        }
        for (Long candidate : candidates) {
            Long resolved = verifyProjectCode(candidate);
            if (resolved != null) {
                return resolved;
            }
        }
        Long existingProjectCode = findProjectCodeByName(projectName);
        if (existingProjectCode != null) {
            return existingProjectCode;
        }
        return createProject(definition, projectName);
    }

    private long resolveProjectCodeForDelete(WorkflowDefinitionDTO definition, Map<String, Object> syncState) {
        Long syncProjectCode = asLong(syncState == null ? null : syncState.get(PROJECT_CODE_KEY));
        if (syncProjectCode != null) {
            Long resolved = verifyProjectCode(syncProjectCode);
            if (resolved != null) {
                return resolved;
            }
        }
        WorkflowEngineBindingDTO binding = definition.getEngineBinding();
        Long bindingProjectCode = binding == null ? null : asLong(binding.getExternalProjectCode());
        if (bindingProjectCode != null) {
            Long resolved = verifyProjectCode(bindingProjectCode);
            if (resolved != null) {
                return resolved;
            }
        }
        throw new IllegalStateException("未能找到 DolphinScheduler 项目编码，无法删除外部工作流");
    }

    private String resolveNamespace(WorkflowDefinitionDTO definition, Map<String, Object> syncState) {
        String namespace = valueOf(syncState.get(NAMESPACE_KEY));
        if (StringUtils.hasText(namespace)) {
            return namespace;
        }
        WorkflowEngineBindingDTO binding = definition.getEngineBinding();
        if (binding != null && StringUtils.hasText(binding.getExternalNamespace())) {
            return binding.getExternalNamespace();
        }
        return normalizeNamespace(definition);
    }

    private long createProject(WorkflowDefinitionDTO definition, String projectName) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("projectName", projectName);
        params.add("description", StringUtils.hasText(definition.getDescription())
                ? definition.getDescription()
                : projectName);
        JsonNode response = apiSupport.postFormJson("/projects", params);
        if (!isSuccess(response)) {
            Long existingProjectCode = findProjectCodeByName(projectName);
            if (existingProjectCode != null) {
                return existingProjectCode;
            }
            throw new IllegalStateException("DolphinScheduler 创建项目失败：" + extractMessage(response));
        }
        return extractLong(response, "code", "projectCode", "id");
    }

    private RemoteWorkflowDefinition syncChildWorkflow(WorkflowDefinitionDTO definition,
                                                       WorkflowStageDTO stage,
                                                       long projectCode,
                                                       Map<String, Object> previousState) {
        String fingerprint = fingerprintForChild(definition, stage);
        Long workflowCode = previousState == null ? null : asLong(previousState.get(WORKFLOW_CODE_KEY));
        Integer workflowVersionNo = previousState == null ? null : asInteger(previousState.get(WORKFLOW_VERSION_KEY));
        boolean remoteExists = workflowCode != null && workflowDefinitionExists(projectCode, workflowCode);
        if (StringUtils.hasText(fingerprint)
                && previousState != null
                && fingerprint.equals(valueOf(previousState.get(FINGERPRINT_KEY)))
                && workflowCode != null
                && workflowVersionNo != null
                && remoteExists) {
            return RemoteWorkflowDefinition.fromState(workflowCode, workflowVersionNo, fingerprint, previousState);
        }
        if (workflowCode != null && !remoteExists) {
            workflowCode = null;
        }
        long startTaskCode = nextCode();
        long endTaskCode = nextCode();
        List<Map<String, Object>> taskDefinitions = buildChildTaskDefinitions(definition, stage, projectCode,
                startTaskCode, endTaskCode);
        List<Map<String, Object>> taskRelations = buildLinearRelations(definition, projectCode, workflowCode,
                startTaskCode, endTaskCode, "CHILD");
        List<Map<String, Object>> locations = buildLocations(startTaskCode, endTaskCode);
        JsonNode response = syncWorkflow(projectCode,
                workflowCode,
                buildChildWorkflowName(definition, stage),
                stage.getDescription(),
                taskDefinitions,
                taskRelations,
                locations);
        long childWorkflowCode = extractWorkflowCode(response);
        Integer childWorkflowVersion = extractWorkflowVersion(response);
        Map<String, Long> codes = new LinkedHashMap<>();
        codes.put("start", startTaskCode);
        codes.put("end", endTaskCode);
        return new RemoteWorkflowDefinition(childWorkflowCode, childWorkflowVersion, fingerprint, codes);
    }

    private RemoteWorkflowDefinition syncParentWorkflow(WorkflowDefinitionDTO definition,
                                                        long projectCode,
                                                        String namespace,
                                                        Map<String, Long> childWorkflowCodes,
                                                        Map<String, Object> previousState) {
        String fingerprint = fingerprintForParent(definition, childWorkflowCodes);
        Long workflowCode = previousState == null ? null : asLong(previousState.get(WORKFLOW_CODE_KEY));
        Integer workflowVersionNo = previousState == null ? null : asInteger(previousState.get(WORKFLOW_VERSION_KEY));
        boolean remoteExists = workflowCode != null && workflowDefinitionExists(projectCode, workflowCode);
        if (StringUtils.hasText(fingerprint)
                && previousState != null
                && fingerprint.equals(valueOf(previousState.get(FINGERPRINT_KEY)))
                && workflowCode != null
                && workflowVersionNo != null
                && remoteExists) {
            return RemoteWorkflowDefinition.fromState(workflowCode, workflowVersionNo, fingerprint, previousState);
        }
        if (workflowCode != null && !remoteExists) {
            workflowCode = null;
        }
        List<WorkflowStageDTO> stages = orderedStages(definition);
        List<Map<String, Object>> taskDefinitions = new ArrayList<>();
        List<Long> taskCodes = new ArrayList<>();
        Map<String, Long> parentTaskCodes = new LinkedHashMap<>();
        for (WorkflowStageDTO stage : stages) {
            long taskCode = nextCode();
            taskCodes.add(taskCode);
            parentTaskCodes.put(stage.getStageCode(), taskCode);
            taskDefinitions.add(buildSubWorkflowTaskDefinition(definition, stage, projectCode, taskCode,
                    childWorkflowCodes.get(stage.getStageCode())));
        }
        List<Map<String, Object>> taskRelations = buildStageChainRelations(definition, projectCode, workflowCode,
                taskCodes);
        List<Map<String, Object>> locations = buildLocations(taskCodes);
        JsonNode response = syncWorkflow(projectCode,
                workflowCode,
                definition.getWorkflowName(),
                definition.getDescription(),
                taskDefinitions,
                taskRelations,
                locations);
        long parentWorkflowCode = extractWorkflowCode(response);
        Integer parentWorkflowVersion = extractWorkflowVersion(response);
        return new RemoteWorkflowDefinition(parentWorkflowCode, parentWorkflowVersion, fingerprint, parentTaskCodes);
    }

    private JsonNode syncWorkflow(long projectCode,
                                  Long workflowCode,
                                  String name,
                                  String description,
                                  List<Map<String, Object>> taskDefinitions,
                                  List<Map<String, Object>> taskRelations,
                                  List<Map<String, Object>> locations) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", name);
        params.add("description", StringUtils.hasText(description) ? description : name);
        params.add("globalParams", "[]");
        params.add("locations", apiSupport.getObjectMapper().valueToTree(locations).toString());
        params.add("timeout", "0");
        params.add("taskRelationJson", apiSupport.getObjectMapper().valueToTree(taskRelations).toString());
        params.add("taskDefinitionJson", apiSupport.getObjectMapper().valueToTree(taskDefinitions).toString());
        params.add("executionType", "PARALLEL");
        params.add("releaseState", "OFFLINE");
        JsonNode response;
        if (workflowCode == null) {
            response = apiSupport.postFormJson("/projects/{projectCode}/workflow-definition", params, projectCode);
        } else {
            response = apiSupport.putFormJson("/projects/{projectCode}/workflow-definition/{workflowCode}",
                params,
                projectCode,
                workflowCode);
        }
        if (!isSuccess(response)) {
            throw new IllegalStateException("DolphinScheduler 工作流发布失败：" + extractMessage(response));
        }
        return response;
    }

    private boolean workflowDefinitionExists(long projectCode, long workflowCode) {
        try {
            JsonNode response = apiSupport.getJson("/projects/{projectCode}/workflow-definition/{workflowCode}",
                    null,
                    projectCode,
                    workflowCode);
            return isSuccess(response);
        } catch (RestClientException exception) {
            return false;
        }
    }

    private void deleteWorkflowDefinition(long projectCode, long workflowCode) {
        JsonNode response = apiSupport.deleteJson("/projects/{projectCode}/workflow-definition/{workflowCode}",
                projectCode,
                workflowCode);
        if (!isSuccess(response)) {
            String message = extractMessage(response);
            if (StringUtils.hasText(message) && message.contains("not exist")) {
                return;
            }
            throw new IllegalStateException("DolphinScheduler 删除工作流失败：" + message);
        }
    }

    private void onlineWorkflowDefinition(long projectCode, long workflowCode, String name) {
        JsonNode response = apiSupport.postFormJson("/projects/{projectCode}/workflow-definition/{workflowCode}/release",
                buildReleaseParams(name, "ONLINE"),
                projectCode,
                workflowCode);
        if (!isSuccess(response)) {
            throw new IllegalStateException("DolphinScheduler 工作流上线失败：" + extractMessage(response));
        }
    }

    private void offlineWorkflowDefinition(long projectCode, long workflowCode, String name) {
        JsonNode response = apiSupport.postFormJson("/projects/{projectCode}/workflow-definition/{workflowCode}/offline",
                buildReleaseParams(name, "OFFLINE"),
                projectCode,
                workflowCode);
        if (!isSuccess(response)) {
            throw new IllegalStateException("DolphinScheduler 工作流下线失败：" + extractMessage(response));
        }
    }

    private WorkflowDefinitionDTO requireSyncedDefinition(WorkflowDefinitionDTO definition) {
        if (definition == null || definition.getEngineBinding() == null) {
            throw new IllegalArgumentException("DolphinScheduler 需要先同步工作流");
        }
        return definition;
    }

    private long resolveProjectCodeForOnlineOffline(WorkflowDefinitionDTO definition) {
        WorkflowEngineBindingDTO binding = definition == null ? null : definition.getEngineBinding();
        if (binding == null || !StringUtils.hasText(binding.getExternalProjectCode())) {
            throw new IllegalArgumentException("DolphinScheduler 需要先同步工作流");
        }
        return Long.parseLong(binding.getExternalProjectCode());
    }

    private List<Map<String, Object>> buildChildTaskDefinitions(WorkflowDefinitionDTO definition,
                                                                WorkflowStageDTO stage,
                                                                long projectCode,
                                                                long startTaskCode,
                                                                long endTaskCode) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        tasks.add(buildShellTaskDefinition(definition, stage, projectCode, startTaskCode,
                stage.getStageName() + "-开始", "echo \"stage start: " + stage.getStageCode() + "\""));
        tasks.add(buildShellTaskDefinition(definition, stage, projectCode, endTaskCode,
                stage.getStageName() + "-结束", "echo \"stage end: " + stage.getStageCode() + "\""));
        return tasks;
    }

    private Map<String, Object> buildShellTaskDefinition(WorkflowDefinitionDTO definition,
                                                         WorkflowStageDTO stage,
                                                         long projectCode,
                                                         long taskCode,
                                                         String taskName,
                                                         String rawScript) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("code", taskCode);
        task.put("version", 1);
        task.put("name", taskName);
        task.put("description", StringUtils.hasText(stage.getDescription()) ? stage.getDescription() : taskName);
        task.put("projectCode", projectCode);
        task.put("taskType", "SHELL");
        task.put("taskParams", Map.of(
                "rawScript", rawScript,
                "resourceList", List.of(),
                "localParams", List.of()));
        task.put("flag", "YES");
        task.put("taskPriority", "MEDIUM");
        task.put("workerGroup", defaultWorkerGroup(definition));
        task.put("environmentCode", 0);
        task.put("failRetryTimes", 0);
        task.put("failRetryInterval", 1);
        task.put("timeout", 0);
        task.put("timeoutFlag", "CLOSE");
        task.put("timeoutNotifyStrategy", "WARN");
        task.put("resourceIds", "");
        task.put("taskGroupId", 0);
        task.put("taskGroupPriority", 0);
        task.put("cpuQuota", null);
        task.put("memoryMax", null);
        task.put("delayTime", 0);
        task.put("createTime", null);
        task.put("updateTime", null);
        task.put("taskExecuteType", "PARALLEL");
        return task;
    }

    private Map<String, Object> buildSubWorkflowTaskDefinition(WorkflowDefinitionDTO definition,
                                                                WorkflowStageDTO stage,
                                                                long projectCode,
                                                                long taskCode,
                                                                Long childWorkflowCode) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("code", taskCode);
        task.put("version", 1);
        task.put("name", stage.getStageName());
        task.put("description", StringUtils.hasText(stage.getDescription()) ? stage.getDescription() : stage.getStageName());
        task.put("projectCode", projectCode);
        task.put("taskType", "SUB_WORKFLOW");
        task.put("taskParams", Map.of(
                "workflowDefinitionCode", childWorkflowCode == null ? 0L : childWorkflowCode,
                "localParams", List.of()));
        task.put("flag", "YES");
        task.put("taskPriority", "MEDIUM");
        task.put("workerGroup", defaultWorkerGroup(definition));
        task.put("environmentCode", 0);
        task.put("failRetryTimes", 0);
        task.put("failRetryInterval", 1);
        task.put("timeout", 0);
        task.put("timeoutFlag", "CLOSE");
        task.put("timeoutNotifyStrategy", "WARN");
        task.put("resourceIds", "");
        task.put("taskGroupId", 0);
        task.put("taskGroupPriority", 0);
        task.put("cpuQuota", null);
        task.put("memoryMax", null);
        task.put("delayTime", 0);
        task.put("createTime", null);
        task.put("updateTime", null);
        task.put("taskExecuteType", "PARALLEL");
        return task;
    }

    private List<Map<String, Object>> buildLinearRelations(WorkflowDefinitionDTO definition,
                                                           long projectCode,
                                                           Long workflowCode,
                                                           long startTaskCode,
                                                           long endTaskCode,
                                                           String relationPrefix) {
        List<Map<String, Object>> relations = new ArrayList<>();
        relations.add(buildRelation(definition, projectCode, workflowCode, 0L, startTaskCode, relationPrefix + "-1"));
        relations.add(buildRelation(definition, projectCode, workflowCode, startTaskCode, endTaskCode,
                relationPrefix + "-2"));
        return relations;
    }

    private List<Map<String, Object>> buildStageChainRelations(WorkflowDefinitionDTO definition,
                                                              long projectCode,
                                                              Long workflowCode,
                                                              List<Long> taskCodes) {
        List<Map<String, Object>> relations = new ArrayList<>();
        if (taskCodes.isEmpty()) {
            return relations;
        }
        relations.add(buildRelation(definition, projectCode, workflowCode, 0L, taskCodes.get(0), "PARENT-1"));
        for (int i = 1; i < taskCodes.size(); i++) {
            relations.add(buildRelation(definition, projectCode, workflowCode, taskCodes.get(i - 1), taskCodes.get(i),
                    "PARENT-" + (i + 1)));
        }
        return relations;
    }

    private Map<String, Object> buildRelation(WorkflowDefinitionDTO definition,
                                              long projectCode,
                                              Long workflowCode,
                                              long preTaskCode,
                                              long postTaskCode,
                                              String relationName) {
        Map<String, Object> relation = new LinkedHashMap<>();
        relation.put("name", relationName);
        relation.put("workflowDefinitionVersion", 1);
        relation.put("projectCode", projectCode);
        relation.put("workflowDefinitionCode", workflowCode == null ? 0L : workflowCode);
        relation.put("preTaskCode", preTaskCode);
        relation.put("preTaskVersion", 1);
        relation.put("postTaskCode", postTaskCode);
        relation.put("postTaskVersion", 1);
        relation.put("conditionType", null);
        relation.put("conditionParams", null);
        relation.put("createTime", null);
        relation.put("updateTime", null);
        return relation;
    }

    private List<Map<String, Object>> buildLocations(long... taskCodes) {
        List<Map<String, Object>> locations = new ArrayList<>();
        for (int index = 0; index < taskCodes.length; index++) {
            locations.add(buildLocation(taskCodes[index], index * 240, 240));
        }
        return locations;
    }

    private List<Map<String, Object>> buildLocations(List<Long> taskCodes) {
        List<Map<String, Object>> locations = new ArrayList<>();
        for (int index = 0; index < taskCodes.size(); index++) {
            locations.add(buildLocation(taskCodes.get(index), index * 240, 240));
        }
        return locations;
    }

    private Map<String, Object> buildLocation(long taskCode, int x, int y) {
        Map<String, Object> location = new LinkedHashMap<>();
        location.put("taskCode", taskCode);
        location.put("x", x);
        location.put("y", y);
        return location;
    }

    private List<WorkflowStageDTO> orderedStages(WorkflowDefinitionDTO definition) {
        List<WorkflowStageDTO> stages = new ArrayList<>(
                definition.getStages() == null ? List.of() : definition.getStages());
        stages.sort(Comparator.comparing(stage -> stage.getStageOrder() == null ? Integer.MAX_VALUE : stage.getStageOrder()));
        return stages;
    }

    private String normalizeProjectName(WorkflowDefinitionDTO definition) {
        return sanitize(definition.getWorkflowCode()) + "-project";
    }

    private String normalizeNamespace(WorkflowDefinitionDTO definition) {
        return sanitize(definition.getWorkflowCode()) + "-ns";
    }

    private String buildChildWorkflowName(WorkflowDefinitionDTO definition, WorkflowStageDTO stage) {
        return sanitize(definition.getWorkflowName()) + "-" + sanitize(stage.getStageCode());
    }

    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return "workflow";
        }
        return value.trim().replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]", "_");
    }

    private long extractWorkflowCode(JsonNode response) {
        JsonNode data = response == null ? null : response.path("data");
        if (data == null || data.isMissingNode() || data.isNull()) {
            data = response == null ? null : response.path("dataList");
        }
        if (data != null && data.isObject()) {
            JsonNode codeNode = data.get("code");
            if (codeNode != null && codeNode.canConvertToLong()) {
                return codeNode.asLong();
            }
            JsonNode workflowCodeNode = data.get("workflowDefinitionCode");
            if (workflowCodeNode != null && workflowCodeNode.canConvertToLong()) {
                return workflowCodeNode.asLong();
            }
        }
        return extractLong(response, "code", "workflowDefinitionCode", "id");
    }

    private Integer extractWorkflowVersion(JsonNode response) {
        JsonNode data = response == null ? null : response.path("data");
        if (data != null && data.isObject()) {
            JsonNode versionNode = data.get("version");
            if (versionNode != null && versionNode.canConvertToInt()) {
                return versionNode.asInt();
            }
            JsonNode workflowNode = data.get("workflowDefinition");
            if (workflowNode != null && workflowNode.isObject()) {
                JsonNode nestedVersion = workflowNode.get("version");
                if (nestedVersion != null && nestedVersion.canConvertToInt()) {
                    return nestedVersion.asInt();
                }
            }
        }
        JsonNode dataList = response == null ? null : response.path("dataList");
        if (dataList != null && dataList.isObject()) {
            JsonNode versionNode = dataList.get("version");
            if (versionNode != null && versionNode.canConvertToInt()) {
                return versionNode.asInt();
            }
        }
        return 1;
    }

    private long extractLong(JsonNode response, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = response == null ? null : response.path("data").path(fieldName);
            if (node != null && node.canConvertToLong()) {
                return node.asLong();
            }
            JsonNode dataNode = response == null ? null : response.path("dataList");
            if (dataNode != null && dataNode.isObject()) {
                JsonNode nested = dataNode.get(fieldName);
                if (nested != null && nested.canConvertToLong()) {
                    return nested.asLong();
                }
            }
        }
        throw new IllegalStateException("未能从 DolphinScheduler 返回值中解析出工作流代码");
    }

    private Long verifyProjectCode(long projectCode) {
        JsonNode response = apiSupport.getJson("/projects/{projectCode}", null, projectCode);
        if (!isSuccess(response)) {
            return null;
        }
        JsonNode data = response.path("data");
        if (data != null && data.isObject()) {
            JsonNode codeNode = data.get("code");
            if (codeNode != null && codeNode.canConvertToLong()) {
                return codeNode.asLong();
            }
        }
        return projectCode;
    }

    private Long findProjectCodeByName(String projectName) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("pageNo", "1");
        params.add("pageSize", "20");
        params.add("searchVal", projectName);
        JsonNode response = apiSupport.getJson("/projects", params);
        if (!isSuccess(response)) {
            return null;
        }
        JsonNode totalList = response.path("data").path("totalList");
        if (!totalList.isArray()) {
            return null;
        }
        for (JsonNode item : totalList) {
            if (item != null && item.isObject()) {
                String name = textOf(item.get("name"));
                if (projectName.equals(name)) {
                    JsonNode codeNode = item.get("code");
                    if (codeNode != null && codeNode.canConvertToLong()) {
                        return codeNode.asLong();
                    }
                }
            }
        }
        if (totalList.size() > 0) {
            JsonNode first = totalList.get(0);
            if (first != null && first.isObject()) {
                JsonNode codeNode = first.get("code");
                if (codeNode != null && codeNode.canConvertToLong()) {
                    return codeNode.asLong();
                }
            }
        }
        return null;
    }

    private boolean isSuccess(JsonNode response) {
        return response != null
                && response.has("code")
                && response.path("code").canConvertToInt()
                && response.path("code").asInt() == 0;
    }

    private String extractMessage(JsonNode response) {
        if (response == null) {
            return "无响应";
        }
        String message = textOf(response.path("msg"));
        if (StringUtils.hasText(message)) {
            return message;
        }
        JsonNode data = response.path("data");
        if (data != null && data.isObject()) {
            String dataMessage = textOf(data.get("msg"));
            if (StringUtils.hasText(dataMessage)) {
                return dataMessage;
            }
        }
        return response.toString();
    }

    private Long resolveWorkflowCodeForDelete(WorkflowDefinitionDTO definition, Map<String, Object> syncState) {
        Long workflowCode = asLong(syncState == null ? null : readStateValue(syncState, PARENT_STATE_KEY, WORKFLOW_CODE_KEY));
        if (workflowCode != null) {
            return workflowCode;
        }
        WorkflowEngineBindingDTO binding = definition.getEngineBinding();
        return binding == null ? null : asLong(binding.getExternalWorkflowId());
    }

    private List<Long> collectChildWorkflowCodes(Map<String, Object> syncState) {
        if (syncState == null) {
            return List.of();
        }
        Map<String, Object> stageStates = asMap(syncState.get(STAGES_STATE_KEY));
        if (stageStates == null || stageStates.isEmpty()) {
            return List.of();
        }
        List<Long> codes = new ArrayList<>();
        for (Object state : stageStates.values()) {
            Map<String, Object> stateMap = asMap(state);
            Long code = asLong(stateMap == null ? null : stateMap.get(WORKFLOW_CODE_KEY));
            if (code != null && !codes.contains(code)) {
                codes.add(code);
            }
        }
        return codes;
    }

    private Object readStateValue(Map<String, Object> syncState, String stateKey, String fieldKey) {
        Object state = syncState.get(stateKey);
        Map<String, Object> stateMap = asMap(state);
        return stateMap == null ? null : stateMap.get(fieldKey);
    }

    private Map<String, Object> readSyncState(WorkflowDefinitionDTO definition) {
        if (definition == null || definition.getEngineBinding() == null || definition.getEngineBinding().getAttributes() == null) {
            return null;
        }
        return asMap(definition.getEngineBinding().getAttributes().get(SYNC_STATE_KEY));
    }

    private MultiValueMap<String, String> buildReleaseParams(String name, String releaseState) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", StringUtils.hasText(name) ? name : "");
        params.add("releaseState", StringUtils.hasText(releaseState) ? releaseState : "ONLINE");
        return params;
    }

    private String textOf(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return StringUtils.hasText(text) ? text : null;
    }

    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return null;
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Long.parseLong(text);
    }

    private String valueOf(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = valueOf(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Integer.parseInt(text);
    }

    private Map<String, Object> readParentState(Map<String, Object> syncState) {
        Map<String, Object> parentState = asMap(syncState.get(PARENT_STATE_KEY));
        if (parentState != null) {
            return parentState;
        }
        parentState = new LinkedHashMap<>();
        if (syncState.get(WORKFLOW_CODE_KEY) != null) {
            parentState.put(WORKFLOW_CODE_KEY, syncState.get(WORKFLOW_CODE_KEY));
        }
        if (syncState.get(WORKFLOW_VERSION_KEY) != null) {
            parentState.put(WORKFLOW_VERSION_KEY, syncState.get(WORKFLOW_VERSION_KEY));
        }
        if (syncState.get(FINGERPRINT_KEY) != null) {
            parentState.put(FINGERPRINT_KEY, syncState.get(FINGERPRINT_KEY));
        }
        if (syncState.get(TASK_CODES_KEY) != null) {
            parentState.put(TASK_CODES_KEY, syncState.get(TASK_CODES_KEY));
        } else if (syncState.get("parentTaskCodes") != null) {
            parentState.put(TASK_CODES_KEY, syncState.get("parentTaskCodes"));
        }
        return parentState;
    }

    private Map<String, Map<String, Object>> readStageStates(Map<String, Object> syncState) {
        Map<String, Map<String, Object>> stageStates = new LinkedHashMap<>();
        if (syncState == null) {
            return stageStates;
        }
        Map<String, Object> nested = asMap(syncState.get(STAGES_STATE_KEY));
        if (nested != null) {
            nested.forEach((key, value) -> {
                Map<String, Object> stageState = asMap(value);
                if (stageState != null) {
                    stageStates.put(String.valueOf(key), stageState);
                }
            });
        }
        if (stageStates.isEmpty()) {
            syncState.forEach((key, value) -> {
                if (PROJECT_CODE_KEY.equals(key)
                        || NAMESPACE_KEY.equals(key)
                        || PARENT_STATE_KEY.equals(key)
                        || STAGES_STATE_KEY.equals(key)
                        || WORKFLOW_CODE_KEY.equals(key)
                        || WORKFLOW_VERSION_KEY.equals(key)
                        || FINGERPRINT_KEY.equals(key)
                        || TASK_CODES_KEY.equals(key)) {
                    return;
                }
                Map<String, Object> stageState = asMap(value);
                if (stageState != null) {
                    stageStates.put(String.valueOf(key), stageState);
                }
            });
        }
        return stageStates;
    }

    private WorkflowStageDTO findStageByCode(WorkflowDefinitionDTO definition, String stageCode) {
        List<WorkflowStageDTO> stages = orderedStages(definition);
        if (StringUtils.hasText(stageCode)) {
            for (WorkflowStageDTO stage : stages) {
                if (stage != null && stageCode.equals(stage.getStageCode())) {
                    return stage;
                }
            }
        }
        return stages.isEmpty() ? null : stages.get(0);
    }

    private String fingerprintForChild(WorkflowDefinitionDTO definition, WorkflowStageDTO stage) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("workflowCode", definition.getWorkflowCode());
        snapshot.put("workflowName", definition.getWorkflowName());
        snapshot.put("workflowVersionNo", definition.getWorkflowVersionNo());
        snapshot.put("stageCode", stage.getStageCode());
        snapshot.put("stageName", stage.getStageName());
        snapshot.put("stageOrder", stage.getStageOrder());
        snapshot.put("description", stage.getDescription());
        snapshot.put("retryable", stage.isRetryable());
        snapshot.put("timeoutSeconds", stage.getTimeoutSeconds());
        return apiSupport.getObjectMapper().valueToTree(snapshot).toString();
    }

    private String fingerprintForParent(WorkflowDefinitionDTO definition, Map<String, Long> childWorkflowCodes) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("workflowCode", definition.getWorkflowCode());
        snapshot.put("workflowName", definition.getWorkflowName());
        snapshot.put("workflowVersionNo", definition.getWorkflowVersionNo());
        snapshot.put("description", definition.getDescription());
        snapshot.put("childWorkflowCodes", childWorkflowCodes);
        snapshot.put("stageCodes", orderedStages(definition).stream().map(WorkflowStageDTO::getStageCode).toList());
        return apiSupport.getObjectMapper().valueToTree(snapshot).toString();
    }

    private String defaultWorkerGroup(WorkflowDefinitionDTO definition) {
        return "default";
    }

    private long nextCode() {
        Set<Long> attempts = new HashSet<>();
        long code;
        do {
            code = Math.abs(ThreadLocalRandom.current().nextLong(100000000000L, Long.MAX_VALUE));
        } while (!attempts.add(code));
        return code;
    }

    private static final class RemoteWorkflowDefinition {
        private final long workflowCode;
        private final Integer workflowVersionNo;
        private final String fingerprint;
        private final Map<String, Long> taskCodes;

        private RemoteWorkflowDefinition(long workflowCode, Integer workflowVersionNo, String fingerprint, Map<String, Long> taskCodes) {
            this.workflowCode = workflowCode;
            this.workflowVersionNo = workflowVersionNo;
            this.fingerprint = fingerprint;
            this.taskCodes = taskCodes;
        }

        private static RemoteWorkflowDefinition fromState(long workflowCode,
                                                          Integer workflowVersionNo,
                                                          String fingerprint,
                                                          Map<String, Object> state) {
            Map<String, Long> taskCodes = new LinkedHashMap<>();
            Map<String, Object> storedTaskCodes = state == null ? null : asMap(state.get(TASK_CODES_KEY));
            if (storedTaskCodes != null) {
                storedTaskCodes.forEach((key, value) -> {
                    if (value instanceof Number number) {
                        taskCodes.put(String.valueOf(key), number.longValue());
                    } else if (value != null) {
                        taskCodes.put(String.valueOf(key), Long.parseLong(String.valueOf(value)));
                    }
                });
            }
            return new RemoteWorkflowDefinition(workflowCode, workflowVersionNo, fingerprint, taskCodes);
        }

        private Map<String, Object> toStateMap() {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put(WORKFLOW_CODE_KEY, workflowCode);
            state.put(WORKFLOW_VERSION_KEY, workflowVersionNo);
            state.put(FINGERPRINT_KEY, fingerprint);
            state.put(TASK_CODES_KEY, taskCodes);
            return state;
        }
    }
}
