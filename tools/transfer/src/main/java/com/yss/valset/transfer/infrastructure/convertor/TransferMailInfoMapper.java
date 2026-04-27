package com.yss.valset.transfer.infrastructure.convertor;

import com.yss.valset.transfer.domain.model.TransferMailInfo;
import com.yss.valset.transfer.infrastructure.entity.TransferMailInfoPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 分拣对象邮件信息映射器。
 */
@Mapper(componentModel = "spring")
public interface TransferMailInfoMapper {

    @Mapping(target = "transferId", expression = "java(mailInfo.transferId())")
    TransferMailInfoPO toPO(TransferMailInfo mailInfo);

    TransferMailInfo toDomain(TransferMailInfoPO po);
}
