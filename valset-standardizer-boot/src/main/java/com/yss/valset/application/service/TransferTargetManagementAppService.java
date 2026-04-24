package com.yss.valset.application.service;

import com.yss.valset.application.command.TransferTargetUpsertCommand;
import com.yss.valset.application.dto.TransferTargetMutationResponse;
import com.yss.valset.application.dto.TransferTargetViewDTO;

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
