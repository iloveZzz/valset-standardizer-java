package com.yss.valset.filemanage.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 源文件收取管理列表视图，字段保持兼容前端生成 API。
 */
@Data
@Builder
public class SourceFileManageVO implements java.io.Serializable {

    /** 主键ID */
    private String id;

    /** 文件名称 */
    private String fileName;

    /** 产品名称 */
    private String pdName;

    /** 产品代码 */
    private String pdCd;

    /** 机构名称 */
    private String orgName;

    /** 机构代码 */
    private String orgCd;

    /** 文件接收时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime fileReceiveTime;

    /** 业务日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate bizDate;

    /** 文件接收渠道 */
    private String fileReceiveChannel;

    /** 文件类型 */
    private String fileType;

    /** 产品类型 */
    private String pdType;

    /** 最后分析时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastAnalysisTime;

    /** 创建人 */
    private String creater;

    /** 文件状态 */
    private String fileState;

    /** 异常信息 */
    private String exceptionMsg;

    /** 异常编码 */
    private String exceptionCode;

    /** 文件路径 */
    private String fileAddress;
}
