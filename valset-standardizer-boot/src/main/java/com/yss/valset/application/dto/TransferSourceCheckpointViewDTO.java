package com.yss.valset.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 来源扫描检查点视图。
 */
@Data
@Builder
public class TransferSourceCheckpointViewDTO {

    /**
     * 检查点主键。
     */
    private String checkpointId;

    /**
     * 来源主键。
     */
    private String sourceId;

    /**
     * 来源类型。
     */
    private String sourceType;

    /**
     * 检查点键。
     */
    private String checkpointKey;

    /**
     * 检查点值。
     */
    private String checkpointValue;

    /**
     * 检查点元数据。
     */
    private Map<String, Object> checkpointMeta;

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
