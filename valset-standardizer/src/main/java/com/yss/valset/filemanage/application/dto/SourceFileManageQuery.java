package com.yss.valset.filemanage.application.dto;

import lombok.Data;

import java.util.List;

/**
 * 源文件收取管理查询条件，字段保持兼容前端生成 API。
 */
@Data
public class SourceFileManageQuery implements java.io.Serializable {

    /** 机构代码 */
    private String orgCd;

    /** 文件名称 */
    private String fileName;

    /** 产品代码 */
    private String pdCd;

    /** 产品类型 */
    private String pdType;

    /** 文件状态 */
    private String fileState;

    /** 文件接收渠道 */
    private String fileReceiveChannel;

    /** 业务日期开始 */
    private String bizDateStart;

    /** 业务日期结束 */
    private String bizDateEnd;

    /** 文件接收日期开始 */
    private String fileReceiveDateStart;

    /** 文件接收日期结束 */
    private String fileReceiveDateEnd;

    /** 状态分组标识 */
    private String stateGroup;

    /** 异常编码 */
    private String exceptionCode;

    /** 文件路径 */
    private String fileAddress;

    /** 文件接收时间 */
    private String fileReceiveTime;

    /** 文件类型 */
    private String fileType;

    /** ID列表 */
    private List<String> ids;
}
