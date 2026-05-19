package com.yss.valset.qlexpress.domain.runtime;

import java.util.List;

/**
 * QLExpress 已启用函数脚本来源。
 */
public interface QlexpressFunctionScriptProvider {

    List<QlexpressFunctionScript> listEnabledScripts();
}
