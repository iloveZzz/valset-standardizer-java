package com.yss.valset.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.domain.gateway.StandardSubjectGateway;
import com.yss.valset.domain.model.StandardSubject;
import com.yss.valset.extract.repository.convertor.StandardSubjectConvertor;
import com.yss.valset.extract.repository.entity.StandardSubjectPO;
import com.yss.valset.extract.repository.mapper.StandardSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 标准科目落地表网关实现。
 */
@Repository
@RequiredArgsConstructor
public class StandardSubjectGatewayImpl implements StandardSubjectGateway {

    private final StandardSubjectRepository standardSubjectRepository;
    private final StandardSubjectConvertor standardSubjectConvertor;

    @Override
    public List<StandardSubject> findAll() {
        List<StandardSubjectPO> poList = standardSubjectRepository.selectList(
                Wrappers.lambdaQuery(StandardSubjectPO.class).orderByAsc(StandardSubjectPO::getRootCode)
                        .orderByAsc(StandardSubjectPO::getLevel)
                        .orderByAsc(StandardSubjectPO::getStandardCode)
        );
        if (poList == null || poList.isEmpty()) {
            return List.of();
        }
        return poList.stream().map(standardSubjectConvertor::toDomain).toList();
    }

    @Override
    public void replaceAll(List<StandardSubject> standardSubjects) {
        standardSubjectRepository.delete(Wrappers.lambdaQuery(StandardSubjectPO.class));
        if (standardSubjects == null || standardSubjects.isEmpty()) {
            return;
        }
        standardSubjectRepository.insert(standardSubjectConvertor.toPO(standardSubjects));
    }
}
