package com.yss.valset.transfer.application.service;

import com.yss.valset.transfer.application.dto.TransferFormTemplateViewDTO;
import com.yss.valset.transfer.application.dto.TransferFormTemplateGroupDTO;

import java.util.List;

/**
 * Transfer 表单模板查询服务。
 */
public interface TransferFormTemplateQueryService {

    List<TransferFormTemplateViewDTO> listTemplates();

    List<TransferFormTemplateGroupDTO> listGroupedTemplates();

    TransferFormTemplateViewDTO getTemplate(String name);
}
