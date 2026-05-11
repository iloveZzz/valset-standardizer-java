package com.yss.valset.application.dto.workflow;

/**
 * 工作流上下文键名常量。
 *
 * <p>
 * 这里的键名用于把应用层、调度层和工作流引擎层的参数统一到同一份上下文中。
 * 代码里所有构造上下文的地方都应复用这些常量，避免出现拼写不一致或字段重复。
 * </p>
 */
public final class WorkflowContextKeys {

    private WorkflowContextKeys() {
    }

    /** 任务和实例的通用标识。 */
    public static final String TASK_ID = "taskId";
    public static final String INSTANCE_ID = "instanceId";
    public static final String STAGE_CODE = "stageCode";
    public static final String WORKFLOW_CODE = "workflowCode";
    public static final String WORKFLOW_NAME = "workflowName";
    public static final String WORKFLOW_ID = "workflowId";
    public static final String WORKFLOW_VERSION_NO = "workflowVersionNo";
    public static final String WORKFLOW_STAGE_CODE = "workflowStageCode";
    public static final String WORKFLOW_STAGE_NAME = "workflowStageName";
    public static final String WORKFLOW_STAGE_DESCRIPTION = "workflowStageDescription";
    public static final String WORKFLOW_ENGINE_TYPE = "workflowEngineType";
    public static final String WORKFLOW_ENGINE_EXTERNAL_REF = "workflowEngineExternalRef";
    public static final String WORKFLOW_ENGINE_CONFIG_JSON = "workflowEngineConfigJson";
    public static final String WORKFLOW_BINDING_ID = "workflowBindingId";
    public static final String WORKFLOW_BINDING_RESOLVED = "workflowBindingResolved";
    public static final String WORKFLOW_BUSINESS_CONTEXT_JSON = "workflowBusinessContextJson";
    /** 信封结构里的公共上下文和业务上下文分组键。 */
    public static final String COMMON_CONTEXT = "commonContext";
    public static final String BUSINESS_CONTEXT = "businessContext";

    /** 估值文件和工作簿的基础信息。 */
    public static final String DATA_SOURCE_TYPE = "dataSourceType";
    public static final String WORKBOOK_PATH = "workbookPath";
    public static final String FILE_ID = "fileId";
    public static final String FILE_FINGERPRINT = "fileFingerprint";
    public static final String FILE_SIZE_BYTES = "fileSizeBytes";
    public static final String FILESYS_TASK_ID = "filesysTaskId";
    public static final String FILESYS_FILE_ID = "filesysFileId";
    public static final String FILESYS_OBJECT_KEY = "filesysObjectKey";
    public static final String FILESYS_INSTANT_UPLOAD = "filesysInstantUpload";
    public static final String FILE_NAME_ORIGINAL = "fileNameOriginal";
    public static final String SOURCE_CHANNEL = "sourceChannel";
    public static final String CREATED_BY = "createdBy";
    public static final String FORCE_REBUILD = "forceRebuild";
    public static final String SOURCE_URI = "sourceUri";
    public static final String STORAGE_URI = "storageUri";
    public static final String FILE_STATUS = "fileStatus";
    public static final String BUSINESS_KEY = "businessKey";

    /** 工作流外部关联与执行控制参数。 */
    public static final String EXTERNAL_WORKFLOW_ID = "externalWorkflowId";
    public static final String EXTERNAL_INSTANCE_ID = "externalInstanceId";
    public static final String STAGE_ORDER = "stageOrder";
    public static final String RETRYABLE = "retryable";
    public static final String TIMEOUT_SECONDS = "timeoutSeconds";
    public static final String TOP_K = "topK";
    public static final String SPLIT_MODE = "splitMode";

    /** 评估任务使用的双工作簿参数。 */
    public static final String MAPPING_WORKBOOK_PATH = "mappingWorkbookPath";
    public static final String STANDARD_WORKBOOK_PATH = "standardWorkbookPath";
    public static final String STANDARD_SOURCE_TYPE = "standardSourceType";
    public static final String MAX_TUNING_SAMPLES = "maxTuningSamples";
    public static final String MAX_TEST_SAMPLES = "maxTestSamples";

    /** 兼容透传容器。 */
    public static final String CONTEXT = "context";
    public static final String PARAMETERS = "parameters";

    public static final String WORKFLOW_ENGINE_CONTEXT_JSON = WORKFLOW_ENGINE_CONFIG_JSON;
}
