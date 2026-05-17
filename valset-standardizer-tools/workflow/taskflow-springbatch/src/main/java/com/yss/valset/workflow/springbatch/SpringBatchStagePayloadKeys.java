package com.yss.valset.workflow.springbatch;

/**
 * 批量任务 运行结果负载键名常量。
 *
 * <p>这些键名用于阶段输入、输出、元数据和作业级返回值的统一编码，
 * 保证日志、查询接口和测试断言使用同一套字段约定。
 */
final class SpringBatchStagePayloadKeys {

    private SpringBatchStagePayloadKeys() {
    }

    static final String PROCESSED_AT = "processedAt";
    static final String BUSINESS_STAGE = "businessStage";
    static final String STAGE_FAMILY = "stageFamily";
    static final String RESULT = "result";
    static final String PLATFORM_TYPE = "platformType";
    static final String OPERATION_TYPE = "operationType";
    static final String WORKFLOW_KEY = "workflowKey";
    static final String BUSINESS_KEY = "businessKey";
    static final String CONTEXT_ECHO = "contextEcho";
    static final String STAGE_PLAN = "stagePlan";
    static final String QUALITY_CHECKS = "qualityChecks";
    static final String TARGET_TABLES = "targetTables";
    static final String SOURCE_SUMMARY = "sourceSummary";
    static final String TRACE_SUMMARY = "traceSummary";
    static final String STAGE_COMPLETED = "stageCompleted";
    static final String NORMALIZED_STAGE_CODE = "normalizedStageCode";
    static final String DESCRIPTION = "description";
    static final String COMMAND_TYPE = "commandType";
    static final String SUMMARY = "summary";
    static final String HAS_SOURCE = "hasSource";
    static final String HAS_STAGE_CODE = "hasStageCode";
    static final String AUDIT_LEVEL = "auditLevel";

    static final String JOB_NAME = "jobName";
    static final String BATCH_INFRASTRUCTURE = "batchInfrastructure";
    static final String CANONICAL_STATUS = "canonicalStatus";
    static final String JOB_EXECUTION_ID = "jobExecutionId";
    static final String BATCH_STATUS = "batchStatus";
    static final String EXIT_STATUS = "exitStatus";
    static final String JOB_PARAMETERS = "jobParameters";
    static final String STEP_COUNT = "stepCount";

    static final String STAGE_CODE = "stageCode";
    static final String STAGE_NAME = "stageName";
    static final String STAGE_ORDER = "stageOrder";
    static final String STAGE_STATUS = "stageStatus";
    static final String STAGE_EXIT_STATUS = "stageExitStatus";

    static final String READ_COUNT = "readCount";
    static final String WRITE_COUNT = "writeCount";
    static final String COMMIT_COUNT = "commitCount";
    static final String ROLLBACK_COUNT = "rollbackCount";
    static final String FILTER_COUNT = "filterCount";

    static final String INPUT = "input";
    static final String OUTPUT = "output";
    static final String METADATA = "metadata";
    static final String EXECUTION_MESSAGE = "executionMessage";
    static final String EXECUTION_RAW_STATUS = "executionRawStatus";
    static final String EXECUTION_STATUS = "executionStatus";

    static final String INPUT_PREFIX = "input.";
    static final String OUTPUT_PREFIX = "output.";
    static final String META_PREFIX = "meta.";
}
