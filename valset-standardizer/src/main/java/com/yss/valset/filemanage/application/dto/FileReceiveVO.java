package com.yss.valset.filemanage.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件收取统计结果。
 */
@Data
@Builder
public class FileReceiveVO implements java.io.Serializable {

    private Integer totalNum;

    private Integer matchedNum;

    private Integer unMatcheNum;

    private Integer rejectNum;

    private Integer lockedNum;
}
