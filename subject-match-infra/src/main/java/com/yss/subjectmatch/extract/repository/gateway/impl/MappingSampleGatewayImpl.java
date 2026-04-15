package com.yss.subjectmatch.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.subjectmatch.domain.gateway.MappingSampleGateway;
import com.yss.subjectmatch.domain.model.MappingSample;
import com.yss.subjectmatch.extract.repository.convertor.MappingSampleConvertor;
import com.yss.subjectmatch.extract.repository.entity.MappingSamplePO;
import com.yss.subjectmatch.extract.repository.mapper.MappingSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 映射样例落地表网关实现。
 */
@Repository
@RequiredArgsConstructor
public class MappingSampleGatewayImpl implements MappingSampleGateway {

    private final MappingSampleRepository mappingSampleRepository;
    private final MappingSampleConvertor mappingSampleConvertor;

    @Override
    public List<MappingSample> findAll() {
        List<MappingSamplePO> poList = mappingSampleRepository.selectList(
                Wrappers.lambdaQuery(MappingSamplePO.class)
                        .orderByAsc(MappingSamplePO::getOrgName)
                        .orderByAsc(MappingSamplePO::getExternalCode)
        );
        if (poList == null || poList.isEmpty()) {
            return List.of();
        }
        return mappingSampleConvertor.toDomain(poList);
    }

    @Override
    public void replaceAll(List<MappingSample> mappingSamples) {
        mappingSampleRepository.delete(Wrappers.lambdaQuery(MappingSamplePO.class));
        if (mappingSamples == null || mappingSamples.isEmpty()) {
            return;
        }
        List<MappingSamplePO> poList = mappingSampleConvertor.toPO(mappingSamples);
        mappingSampleRepository.insert(poList);
    }
}
