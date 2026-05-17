package com.yss.valset.transfer.infrastructure.dto;

/**
 * 标签识别结果汇总查询 DTO。
 */
public class TransferObjectTagSummaryDTO implements java.io.Serializable {

    private String tagCode;
    private String tagName;
    private Long tagCount;

    public String getTagCode() {
        return tagCode;
    }

    public void setTagCode(String tagCode) {
        this.tagCode = tagCode;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public Long getTagCount() {
        return tagCount;
    }

    public void setTagCount(Long tagCount) {
        this.tagCount = tagCount;
    }
}
