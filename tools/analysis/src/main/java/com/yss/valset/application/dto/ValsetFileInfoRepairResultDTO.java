package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件主数据回填结果。
 */
@Data
@Builder
public class ValsetFileInfoRepairResultDTO {

    /**
     * 是否预览模式。
     */
    private boolean dryRun;

    /**
     * 本次扫描的 transferId。
     */
    private String transferId;

    /**
     * 分批大小。
     */
    private int pageSize;

    /**
     * 扫描到的文件主对象数量。
     */
    private long scannedCount;

    /**
     * 已存在且成功对齐的文件主数据数量。
     */
    private long matchedCount;

    /**
     * 新增的文件主数据数量。
     */
    private long createdCount;

    /**
     * 更新的文件主数据数量。
     */
    private long updatedCount;

    /**
     * 跳过的数量。
     */
    private long skippedCount;

    /**
     * 失败数量。
     */
    private long failedCount;
}
