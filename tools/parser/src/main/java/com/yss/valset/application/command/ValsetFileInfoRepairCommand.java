package com.yss.valset.application.command;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 文件主数据回填命令。
 */
@Data
public class ValsetFileInfoRepairCommand {

    /**
     * 指定待修复的 transferId，留空则全量扫描。
     */
    private String transferId;

    /**
     * 分批扫描数量。
     */
    @Min(value = 1, message = "批量大小必须大于 0")
    @Max(value = 1000, message = "批量大小不能超过 1000")
    private Integer pageSize = 200;

    /**
     * 是否在文件主数据缺失时自动创建。
     */
    private Boolean createMissing = Boolean.TRUE;

    /**
     * 是否只预览不落库。
     */
    private Boolean dryRun = Boolean.FALSE;
}
