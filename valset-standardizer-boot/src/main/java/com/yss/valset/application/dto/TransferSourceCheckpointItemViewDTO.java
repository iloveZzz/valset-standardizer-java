package com.yss.valset.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 来源检查点去重记录视图。
 */
@Data
@Builder
public class TransferSourceCheckpointItemViewDTO {

    /**
     * 去重记录主键。
     */
    private String checkpointItemId;

    /**
     * 来源主键。
     */
    private String sourceId;

    /**
     * 来源类型。
     */
    private String sourceType;

    /**
     * 去重键。
     */
    private String itemKey;

    /**
     * 来源引用标识。
     */
    private String itemRef;

    /**
     * 条目名称。
     */
    private String itemName;

    /**
     * 条目大小。
     */
    private Long itemSize;

    /**
     * 条目 MIME 类型。
     */
    private String itemMimeType;

    /**
     * 条目指纹。
     */
    private String itemFingerprint;

    /**
     * 条目元数据。
     */
    private Map<String, Object> itemMeta;

    /**
     * 触发类型。
     */
    private String triggerType;

    /**
     * 处理时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime processedAt;

    /**
     * 创建时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /**
     * 修改时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
