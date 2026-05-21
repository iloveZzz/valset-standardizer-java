package com.yss.valset.task.application.dto.product;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 产品信息提取保存汇总结果。
 */
@Data
public class ProductInfoExtractionSaveResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int successCount;
    private int skippedCount;
    private int failedCount;
    private List<ProductInfoExtractionSaveItemDTO> items = new ArrayList<>();
}
