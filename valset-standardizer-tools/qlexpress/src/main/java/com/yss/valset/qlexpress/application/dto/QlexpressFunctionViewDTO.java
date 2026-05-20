package com.yss.valset.qlexpress.application.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * QLExpress 自定义函数视图。
 */
@Data
@Builder
public class QlexpressFunctionViewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String functionId;

    private String functionCnName;

    private String functionName;

    private String remark;

    private String scriptBody;

    private Boolean enabled;

    private Object extInfo;

    private List<String> sourceModules;

    private List<String> flowLabels;

    private String usageStatus;

    private String usageStatusName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
