package com.yss.valset.transfer.application.service;

import com.yss.valset.transfer.application.command.TransferTargetUpsertCommand;
import com.yss.valset.transfer.application.dto.TransferTargetMutationResponse;
import com.yss.valset.transfer.application.dto.TransferTargetViewDTO;

import java.util.List;

/**
 * 投递目标管理服务。
 */
public interface TransferTargetManagementAppService {

    List<TransferTargetViewDTO> listTargets(String targetType, String targetCode, Boolean enabled, Integer limit);

    TransferTargetViewDTO getTarget(String targetId);

    TransferTargetMutationResponse upsertTarget(TransferTargetUpsertCommand command);

    TransferTargetMutationResponse deleteTarget(String targetId);
}
