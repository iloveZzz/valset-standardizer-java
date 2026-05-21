package com.yss.valset.task.application.command.product;

import com.yss.valset.task.application.dto.product.ProductInfoExtractionPreviewDTO;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 产品信息提取保存命令。
 */
@Data
public class ProductInfoExtractionSaveCommand {

    @Valid
    @NotEmpty(message = "请选择需要保存的产品信息")
    private List<ProductInfoExtractionPreviewDTO> items;
}
