package com.yss.valset.knowledge;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 知识库导入返回结果。
 */
@Data
@Builder
public class KnowledgeImportResponse implements Serializable {
    /**
     * 目标落地表。
     */
    private String targetTable;
    /**
     * 数据来源类型。
     */
    private String sourceType;
    /**
     * 导入记录数。
     */
    private Long importedCount;
}
