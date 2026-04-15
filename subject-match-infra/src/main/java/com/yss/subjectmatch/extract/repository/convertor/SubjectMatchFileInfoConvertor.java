package com.yss.subjectmatch.extract.repository.convertor;

import com.yss.subjectmatch.domain.model.SubjectMatchFileInfo;
import com.yss.subjectmatch.extract.repository.entity.SubjectMatchFileInfoPO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubjectMatchFileInfoConvertor {
    SubjectMatchFileInfoPO toPO(SubjectMatchFileInfo fileInfo);

    SubjectMatchFileInfo toDomain(SubjectMatchFileInfoPO po);
}
