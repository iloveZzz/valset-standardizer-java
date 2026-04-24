package com.yss.valset.application.service;

import com.yss.valset.application.command.TransferSourceUpsertCommand;
import com.yss.valset.application.dto.TransferSourceCheckpointItemViewDTO;
import com.yss.valset.application.dto.TransferSourceCheckpointViewDTO;
import com.yss.valset.application.dto.TransferSourceMutationResponse;
import com.yss.valset.application.dto.TransferSourceViewDTO;

import java.util.List;

/**
 * 文件来源管理服务。
 */
public interface TransferSourceManagementAppService {

    List<TransferSourceViewDTO> listSources(String sourceType, String sourceCode, String sourceName, Boolean enabled, Integer limit);

    TransferSourceViewDTO getSource(String sourceId);

    TransferSourceMutationResponse upsertSource(TransferSourceUpsertCommand command);

    TransferSourceMutationResponse deleteSource(String sourceId);

    TransferSourceMutationResponse triggerSource(String sourceId);

    TransferSourceMutationResponse stopSource(String sourceId);

    TransferSourceMutationResponse clearProcessedMailIds(String sourceId);

    List<TransferSourceCheckpointViewDTO> listCheckpoints(String sourceId, Integer limit);

    List<TransferSourceCheckpointItemViewDTO> listCheckpointItems(String sourceId, Integer limit);
}
