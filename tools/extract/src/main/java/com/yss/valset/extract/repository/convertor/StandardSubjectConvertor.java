package com.yss.valset.extract.repository.convertor;

import com.yss.valset.domain.model.StandardSubject;
import com.yss.valset.extract.repository.entity.StandardSubjectPO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StandardSubjectConvertor {
    StandardSubjectPO toPO(StandardSubject standardSubject);

    StandardSubject toDomain(StandardSubjectPO po);

    List<StandardSubjectPO> toPO(List<StandardSubject> standardSubjects);
}
