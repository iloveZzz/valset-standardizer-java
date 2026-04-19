package com.yss.valset.extract.repository.convertor;

import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.extract.repository.entity.ValsetFileInfoPO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ValsetFileInfoConvertor {
    ValsetFileInfoPO toPO(ValsetFileInfo fileInfo);

    ValsetFileInfo toDomain(ValsetFileInfoPO po);
}
