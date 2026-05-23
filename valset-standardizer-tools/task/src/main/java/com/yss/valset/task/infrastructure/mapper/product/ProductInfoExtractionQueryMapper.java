package com.yss.valset.task.infrastructure.mapper.product;

import com.yss.valset.task.infrastructure.dto.product.ProductInfoExtractionCandidateRow;
import com.yss.valset.task.application.dto.product.ProductInfoOptionDTO;
import com.yss.valset.task.infrastructure.dto.product.ProductInfoExtractionValuationTitleRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 产品信息提取查询 Mapper。
 */
@Mapper
public interface ProductInfoExtractionQueryMapper {

    long countCandidates(@Param("originalName") String originalName);

    long countProductOptions(@Param("keyword") String keyword);

    List<ProductInfoExtractionCandidateRow> pageCandidates(@Param("originalName") String originalName,
                                                           @Param("offset") int offset,
                                                           @Param("limit") int limit);

    List<ProductInfoOptionDTO> pageProductOptions(@Param("keyword") String keyword,
                                                  @Param("offset") int offset,
                                                  @Param("limit") int limit);

    List<ProductInfoExtractionValuationTitleRow> listValuationTitlesByFileIds(@Param("fileIds") List<Long> fileIds);
}
