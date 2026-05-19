package com.yss.valset.qlexpress.application.service;

import com.yss.cloud.dto.result.PageResult;
import com.yss.valset.qlexpress.application.command.QlexpressFunctionDebugCommand;
import com.yss.valset.qlexpress.application.command.QlexpressFunctionUpsertCommand;
import com.yss.valset.qlexpress.application.dto.QlexpressFunctionDebugResultDTO;
import com.yss.valset.qlexpress.application.dto.QlexpressFunctionMutationResponse;
import com.yss.valset.qlexpress.application.dto.QlexpressFunctionViewDTO;

/**
 * QLExpress 自定义函数管理服务。
 */
public interface QlexpressFunctionManagementAppService {

    PageResult<QlexpressFunctionViewDTO> pageFunctions(String functionCnName, String functionName, Boolean enabled, Integer pageIndex, Integer pageSize);

    QlexpressFunctionViewDTO getFunction(String functionId);

    QlexpressFunctionMutationResponse upsertFunction(QlexpressFunctionUpsertCommand command);

    QlexpressFunctionMutationResponse deleteFunction(String functionId);

    QlexpressFunctionMutationResponse enableFunction(String functionId);

    QlexpressFunctionMutationResponse disableFunction(String functionId);

    QlexpressFunctionDebugResultDTO debug(QlexpressFunctionDebugCommand command);
}
