package com.yss.valset.task.web.controller;

import com.yss.cloud.dto.result.MultiResult;
import com.yss.cloud.dto.result.PageResult;
import com.yss.cloud.dto.result.SingleResult;
import com.yss.valset.task.application.command.product.ProductInfoExtractionPreviewCommand;
import com.yss.valset.task.application.command.product.ProductInfoExtractionSaveCommand;
import com.yss.valset.task.application.dto.product.ProductInfoExtractionCandidateDTO;
import com.yss.valset.task.application.dto.product.ProductInfoExtractionPreviewDTO;
import com.yss.valset.task.application.dto.product.ProductInfoOptionDTO;
import com.yss.valset.task.application.dto.product.ProductInfoExtractionSaveResultDTO;
import com.yss.valset.task.application.service.product.ProductInfoExtractionAppService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 产品信息提取接口。
 */
@RestController
@RequestMapping("/product-info-extractions")
@RequiredArgsConstructor
public class ProductInfoExtractionController {

    private final ProductInfoExtractionAppService productInfoExtractionAppService;

    @GetMapping("/candidates")
    @Operation(summary = "分页查询产品信息提取候选文件")
    public PageResult<ProductInfoExtractionCandidateDTO> pageCandidates(
            @RequestParam(value = "productType", required = false) String productType,
            @RequestParam(value = "originalName", required = false) String originalName,
            @RequestParam(value = "pageIndex", required = false) Integer pageIndex,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return productInfoExtractionAppService.pageCandidates(productType, originalName, pageIndex, pageSize);
    }

    @GetMapping("/options")
    @Operation(summary = "分页查询产品主数据候选项")
    public PageResult<ProductInfoOptionDTO> pageProductOptions(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageIndex", required = false) Integer pageIndex,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return productInfoExtractionAppService.pageProductOptions(keyword, pageIndex, pageSize);
    }

    @PostMapping("/preview")
    @Operation(summary = "生成产品信息提取预览")
    public MultiResult<ProductInfoExtractionPreviewDTO> preview(@Valid @RequestBody ProductInfoExtractionPreviewCommand command) {
        return MultiResult.of(productInfoExtractionAppService.preview(command));
    }

    @PostMapping("/rules")
    @Operation(summary = "保存产品识别规则并补打产品识别标签")
    public SingleResult<ProductInfoExtractionSaveResultDTO> saveRules(@Valid @RequestBody ProductInfoExtractionSaveCommand command) {
        return SingleResult.of(productInfoExtractionAppService.saveRules(command));
    }
}
