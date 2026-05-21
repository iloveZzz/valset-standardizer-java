package com.yss.valset.task.application.service.product;

import com.yss.cloud.dto.result.PageResult;
import com.yss.valset.task.application.command.product.ProductInfoExtractionPreviewCommand;
import com.yss.valset.task.application.command.product.ProductInfoExtractionSaveCommand;
import com.yss.valset.task.application.dto.product.ProductInfoExtractionCandidateDTO;
import com.yss.valset.task.application.dto.product.ProductInfoExtractionPreviewDTO;
import com.yss.valset.task.application.dto.product.ProductInfoOptionDTO;
import com.yss.valset.task.application.dto.product.ProductInfoExtractionSaveResultDTO;

import java.util.List;

/**
 * 产品信息提取应用服务。
 */
public interface ProductInfoExtractionAppService {

    PageResult<ProductInfoExtractionCandidateDTO> pageCandidates(String productType,
                                                                 String originalName,
                                                                 Integer pageIndex,
                                                                 Integer pageSize);

    PageResult<ProductInfoOptionDTO> pageProductOptions(String keyword,
                                                        Integer pageIndex,
                                                        Integer pageSize);

    List<ProductInfoExtractionPreviewDTO> preview(ProductInfoExtractionPreviewCommand command);

    ProductInfoExtractionSaveResultDTO saveRules(ProductInfoExtractionSaveCommand command);
}
