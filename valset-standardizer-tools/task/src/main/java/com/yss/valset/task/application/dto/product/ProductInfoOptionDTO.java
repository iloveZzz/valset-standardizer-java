package com.yss.valset.task.application.dto.product;

import lombok.Data;

import java.io.Serializable;

/**
 * 产品主数据信息候选项。
 */
@Data
public class ProductInfoOptionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String productCode;
    private String productName;
    private String managerCode;
    private String managerName;
}
