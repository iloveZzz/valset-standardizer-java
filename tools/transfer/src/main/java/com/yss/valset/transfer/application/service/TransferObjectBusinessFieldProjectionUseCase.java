package com.yss.valset.transfer.application.service;

import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectTag;

import java.util.List;

/**
 * 文件主对象业务字段投影用例。
 */
public interface TransferObjectBusinessFieldProjectionUseCase {

    TransferObject project(TransferObject transferObject, List<TransferObjectTag> tags);
}
