package com.yss.valset.qlexpress.domain.runtime;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * 待注册的 QLExpress 函数脚本。
 */
@Getter
@AllArgsConstructor
public class QlexpressFunctionScript {

    private final String functionName;

    private final String scriptBody;

    private final List<String> sourceModules;

    public QlexpressFunctionScript(String functionName, String scriptBody) {
        this(functionName, scriptBody, Collections.emptyList());
    }
}
