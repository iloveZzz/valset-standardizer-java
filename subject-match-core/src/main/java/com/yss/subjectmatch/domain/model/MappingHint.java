package com.yss.subjectmatch.domain.model;

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
    private String source;
    private String normalizedKey;
    private String standardCode;
    private String standardName;
    private Integer supportCount;
    private BigDecimal confidence;
}
