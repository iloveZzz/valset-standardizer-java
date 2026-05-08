package com.yss.valset.workflow.springbatch;

import java.util.List;

/**
 * Spring Batch 业务阶段语义。
 *
 * <p>这里把业务阶段码、阶段族、阶段说明、审计等级和目标表集中起来，
 * 便于后续新增阶段时统一维护，而不是把这些语义散落在处理代码里。
 */
enum SpringBatchBusinessStage {

    FILE_PARSE("FILE_PARSE", "EXTRACT", "原始文件抽取与解析已完成", "RAW", List.of("t_ods_valuation_filedata")),
    STRUCTURE_STANDARDIZE("STRUCTURE_STANDARDIZE", "STANDARDIZE", "字段映射与结构标准化已完成", "STAGING",
            List.of("t_stg_external_valuation", "t_stg_external_valuation_detail")),
    SUBJECT_RECOGNIZE("SUBJECT_RECOGNIZE", "RECOGNIZE", "科目识别与标签补全已完成", "MATCH", List.of("t_subject_match_result")),
    STANDARD_LANDING("STANDARD_LANDING", "LANDING", "标准表落地已完成", "DWD",
            List.of("t_dwd_external_valuation", "t_dwd_external_valuation_detail")),
    DATA_PROCESSING("DATA_PROCESSING", "PROCESS", "后续加工任务已完成", "PROCESS",
            List.of("t_valset_parse_task", "t_valset_parse_task_step")),
    VERIFY_ARCHIVE("VERIFY_ARCHIVE", "ARCHIVE", "一致性校验与归档已完成", "ARCHIVE",
            List.of("t_valset_parse_task_log"));

    private final String code;
    private final String family;
    private final String message;
    private final String auditLevel;
    private final List<String> targetTables;

    SpringBatchBusinessStage(String code, String family, String message, String auditLevel, List<String> targetTables) {
        this.code = code;
        this.family = family;
        this.message = message;
        this.auditLevel = auditLevel;
        this.targetTables = targetTables;
    }

    public String code() {
        return code;
    }

    public String family() {
        return family;
    }

    public String message() {
        return message;
    }

    public String auditLevel() {
        return auditLevel;
    }

    public List<String> targetTables() {
        return targetTables;
    }

    public static SpringBatchBusinessStage fromStageCode(String stageCode) {
        if (stageCode == null || stageCode.isBlank()) {
            return FILE_PARSE;
        }
        return switch (stageCode.trim().toUpperCase()) {
            case "RAW_DATA_EXTRACT", "FILE_PARSE" -> FILE_PARSE;
            case "STRUCTURE_STANDARDIZE" -> STRUCTURE_STANDARDIZE;
            case "SUBJECT_RECOGNIZE" -> SUBJECT_RECOGNIZE;
            case "STANDARD_LANDING" -> STANDARD_LANDING;
            case "DATA_PROCESSING" -> DATA_PROCESSING;
            case "VERIFY_ARCHIVE" -> VERIFY_ARCHIVE;
            default -> FILE_PARSE;
        };
    }
}
