package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 将外部主题与标准主题联系起来的历史提示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingHint {
    /** 数据源 */
    private String source;
    /** 规范化键值 */
    private String normalizedKey;
    /** 标准代码 */
    private String standardCode;
    /** 标准名称 */
    private String standardName;
    /** 支持计数 */
    private Integer supportCount;
    /** 置信度 */
    private BigDecimal confidence;
}
