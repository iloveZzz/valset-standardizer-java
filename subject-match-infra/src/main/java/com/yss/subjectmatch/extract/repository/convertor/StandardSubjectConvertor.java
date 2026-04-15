package com.yss.subjectmatch.extract.repository.convertor;

import com.yss.subjectmatch.domain.model.StandardSubject;
import com.yss.subjectmatch.extract.repository.entity.StandardSubjectPO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StandardSubjectConvertor {
    StandardSubjectPO toPO(StandardSubject standardSubject);

    StandardSubject toDomain(StandardSubjectPO po);

    List<StandardSubjectPO> toPO(List<StandardSubject> standardSubjects);
}
