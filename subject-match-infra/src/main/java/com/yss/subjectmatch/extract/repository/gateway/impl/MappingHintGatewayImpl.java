package com.yss.subjectmatch.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.subjectmatch.domain.gateway.MappingHintGateway;
import com.yss.subjectmatch.domain.model.MappingHint;
import com.yss.subjectmatch.extract.repository.convertor.MappingHintConvertor;
import com.yss.subjectmatch.extract.repository.entity.MappingHintPO;
import com.yss.subjectmatch.extract.repository.mapper.MappingHintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 历史映射经验落地表网关实现。
 */
@Repository
@RequiredArgsConstructor
public class MappingHintGatewayImpl implements MappingHintGateway {

    private final MappingHintRepository mappingHintRepository;
    private final MappingHintConvertor mappingHintConvertor;

    @Override
    public List<MappingHint> findAll() {
        List<MappingHintPO> poList = mappingHintRepository.selectList(
                Wrappers.lambdaQuery(MappingHintPO.class)
                        .orderByAsc(MappingHintPO::getNormalizedKey)
                        .orderByAsc(MappingHintPO::getStandardCode)
        );
        if (poList == null || poList.isEmpty()) {
            return List.of();
        }
        return poList.stream().map(mappingHintConvertor::toDomain).toList();
    }

    @Override
    public void replaceAll(List<MappingHint> mappingHints) {
        mappingHintRepository.delete(Wrappers.lambdaQuery(MappingHintPO.class));
        if (mappingHints == null || mappingHints.isEmpty()) {
            return;
        }
        for (MappingHint mappingHint : mappingHints) {
            mappingHintRepository.insert(mappingHintConvertor.toPO(mappingHint));
        }
    }
}
