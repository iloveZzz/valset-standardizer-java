package com.yss.subjectmatch.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.subjectmatch.domain.gateway.ParsedValuationDataGateway;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.extract.repository.mapper.ParsedValuationDataRepository;
import com.yss.subjectmatch.extract.repository.convertor.ParsedValuationDataConvertor;
import com.yss.subjectmatch.extract.repository.entity.ParsedValuationDataPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * MyBatis 支持的解析工作簿网关。
 */
@Repository
@RequiredArgsConstructor
public class ParsedValuationDataGatewayImpl implements ParsedValuationDataGateway {

    private final ParsedValuationDataRepository parsedValuationDataRepository;
    private final ParsedValuationDataConvertor parsedValuationDataConvertor;

    /**
     * 保留已解析的工作簿数据。
     */
    @Override
    public void saveParsedValuationData(Long taskId, Long fileId, ParsedValuationData parsedValuationData) {
        parsedValuationDataRepository.insert(parsedValuationDataConvertor.toPO(taskId, fileId, parsedValuationData));
    }

    /**
     * 按文件标识查询最近一次解析结果。
     */
    @Override
    public ParsedValuationData findLatestByFileId(Long fileId) {
        ParsedValuationDataPO po = parsedValuationDataRepository.selectOne(
                Wrappers.lambdaQuery(ParsedValuationDataPO.class)
                        .eq(ParsedValuationDataPO::getFileId, fileId)
                        .orderByDesc(ParsedValuationDataPO::getId)
                        .last("limit 1")
        );
        return po == null ? null : parsedValuationDataConvertor.toDomain(po);
    }
}
