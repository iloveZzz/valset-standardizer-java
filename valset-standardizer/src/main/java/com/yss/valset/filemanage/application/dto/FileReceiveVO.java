package com.yss.valset.filemanage.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件收取统计结果。
 */
@Data
@Builder
public class FileReceiveVO implements java.io.Serializable {

    /** 总文件数 */
    private Integer totalNum;

    /** 已匹配文件数 */
    private Integer matchedNum;

    /** 未匹配文件数 */
    private Integer unMatcheNum;

    /** 驳回文件数 */
    private Integer rejectNum;

    /** 锁定文件数 */
    private Integer lockedNum;
}
