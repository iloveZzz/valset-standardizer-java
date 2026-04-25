package com.yss.valset.transfer.application.service;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.transfer.application.command.TransferTagTestCommand;
import com.yss.valset.transfer.application.command.TransferTagUpsertCommand;
import com.yss.valset.transfer.application.dto.TransferTagMutationResponse;
import com.yss.valset.transfer.application.dto.TransferTagTestResultDTO;
import com.yss.valset.transfer.application.dto.TransferTagViewDTO;

/**
 * 标签管理服务。
 */
public interface TransferTagManagementAppService {

    PageResult<TransferTagViewDTO> pageTags(String tagCode, String matchStrategy, Boolean enabled, Integer pageIndex, Integer pageSize);

    TransferTagViewDTO getTag(String tagId);

    TransferTagMutationResponse upsertTag(TransferTagUpsertCommand command);

    TransferTagMutationResponse deleteTag(String tagId);

    TransferTagTestResultDTO testTag(String tagId, TransferTagTestCommand command);
}
