package com.yss.valset.qlexpress.application.service;

import com.yss.valset.qlexpress.application.dto.QlexpressFunctionUsageDTO;
import com.yss.valset.qlexpress.infrastructure.entity.QlexpressFunctionPO;

import java.util.List;
import java.util.Map;

/**
 * QLExpress 函数使用关系查询服务。
 */
public interface QlexpressFunctionUsageAppService {

    QlexpressFunctionUsageDTO getUsage(String functionId);

    QlexpressFunctionUsageDTO summarizeUsage(QlexpressFunctionPO function);

    Map<String, QlexpressFunctionUsageDTO> summarizeUsages(List<QlexpressFunctionPO> functions);
}
