package com.yss.valset.task.domain.model;

import lombok.Getter;

/**
 * 估值表解析任务处理阶段。
 */
@Getter
public enum OutsourcedDataTaskStage {

    RAW_DATA_EXTRACT("原始数据提取", "Excel/CSV 原始行提取、ODS 落地"),
    FILE_PARSE("文件解析", "文件识别、Sheet 解析、原始行列抽取"),
    STRUCTURE_STANDARDIZE("结构标准化", "字段映射、数据清洗、STG 结构转换"),
    SUBJECT_RECOGNIZE("科目识别", "科目匹配、属性识别、标签补全"),
    STANDARD_LANDING("标准表落地", "STG/DWD/标准持仓/估值数据写入"),
    DATA_PROCESSING("加工任务", "后续数据加工、补充计算、派生数据生成"),
    VERIFY_ARCHIVE("校验归档", "一致性校验、结果确认、归档完成");

    private final String label;

    private final String description;

    OutsourcedDataTaskStage(String label, String description) {
        this.label = label;
        this.description = description;
    }
}
