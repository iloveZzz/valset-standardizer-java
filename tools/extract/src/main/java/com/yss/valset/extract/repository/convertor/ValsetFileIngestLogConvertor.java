package com.yss.valset.extract.repository.convertor;

import com.yss.valset.domain.model.ValsetFileIngestLog;
import com.yss.valset.extract.repository.entity.ValsetFileIngestLogPO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ValsetFileIngestLogConvertor {
    ValsetFileIngestLogPO toPO(ValsetFileIngestLog ingestLog);

    ValsetFileIngestLog toDomain(ValsetFileIngestLogPO po);
}
