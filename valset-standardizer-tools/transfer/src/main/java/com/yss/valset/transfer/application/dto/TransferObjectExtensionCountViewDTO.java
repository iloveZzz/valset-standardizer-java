package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件后缀统计视图。
 */
@Data
@Builder
public class TransferObjectExtensionCountViewDTO {

    /**
     * 后缀。
     */
    private String extension;

    /**
     * 后缀标签。
     */
    private String extensionLabel;

    /**
     * 数量。
     */
    private Long count;
}
