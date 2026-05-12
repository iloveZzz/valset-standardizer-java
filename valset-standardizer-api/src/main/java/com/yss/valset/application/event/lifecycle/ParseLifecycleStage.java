package com.yss.valset.application.event.lifecycle;

/**
 * 解析生命周期阶段。
 */
public enum ParseLifecycleStage {

    /**
     * 文件解析。
     */
    FILE_PARSE("文件解析", "完成文件读取、原始数据抽取与解析结果生成"),

    /**
     * 结构标准化。
     */
    STRUCTURE_STANDARDIZE("结构标准化", "完成字段标准化、规则清洗与结构转换"),

    /**
     * 标准数据落地。
     */
    STANDARD_LANDING("标准数据落地", "完成标准化结果持久化与结果落库"),

    /**
     * 解析失败。
     */
    FAILED("失败", "解析流程发生失败"),

    /**
     * 解析跳过。
     */
    SKIPPED("跳过", "解析流程被跳过或复用");

    private final String label;

    private final String description;

    ParseLifecycleStage(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
