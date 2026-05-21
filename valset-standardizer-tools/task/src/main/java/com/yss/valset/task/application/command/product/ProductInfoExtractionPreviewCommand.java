package com.yss.valset.task.application.command.product;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 产品信息提取预览命令。
 */
@Data
public class ProductInfoExtractionPreviewCommand {

    private String productType;

    private String extractionStrategy;

    @NotEmpty(message = "请选择文件")
    private List<String> transferIds;
}
