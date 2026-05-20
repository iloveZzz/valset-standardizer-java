package com.yss.valset.extract.support;

/**
 * 标准落地表使用的产品业务字段。
 */
public class ProductBusinessFields {

    private final String productCode;

    private final String orgCode;

    public ProductBusinessFields(String productCode, String orgCode) {
        this.productCode = productCode;
        this.orgCode = orgCode;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getOrgCode() {
        return orgCode;
    }
}
