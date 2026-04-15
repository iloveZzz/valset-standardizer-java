package com.yss.subjectmatch.extract.repository.convertor;

import com.yss.subjectmatch.domain.model.SubjectMatchFileIngestLog;
import com.yss.subjectmatch.extract.repository.entity.SubjectMatchFileIngestLogPO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubjectMatchFileIngestLogConvertor {
    SubjectMatchFileIngestLogPO toPO(SubjectMatchFileIngestLog ingestLog);

    SubjectMatchFileIngestLog toDomain(SubjectMatchFileIngestLogPO po);
}
