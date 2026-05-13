package com.yss.valset.transfer.application.service;

import com.yss.valset.transfer.application.command.TransferTagTestCommand;
import com.yss.valset.transfer.application.dto.TransferTagTestResultDTO;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectTag;

import java.util.List;

/**
 * 文件对象打标用例。
 */
public interface TransferTaggingUseCase {

    List<TransferObjectTag> tag(TransferObject transferObject, RecognitionContext recognitionContext, ProbeResult probeResult);

    List<TransferObjectTag> retag(String transferId, boolean overwrite);

    TransferTagTestResultDTO test(String tagId, TransferTagTestCommand command);
}
