package com.yss.valset.qlexpress.domain.runtime;

import java.util.Map;

/**
 * QLExpress 执行上下文白名单对象贡献器。
 */
public interface QlexpressContextContributor {

    void contribute(String runnerScope, Map<String, Object> context);
}
