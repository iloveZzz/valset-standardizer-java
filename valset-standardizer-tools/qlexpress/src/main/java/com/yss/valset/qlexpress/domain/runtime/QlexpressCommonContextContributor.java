package com.yss.valset.qlexpress.domain.runtime;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 注入所有 QLExpress 场景共享的函数对象。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class QlexpressCommonContextContributor implements QlexpressContextContributor {

    private final QlexpressCommonFunctionFacade commonFunctionFacade;

    public QlexpressCommonContextContributor(QlexpressCommonFunctionFacade commonFunctionFacade) {
        this.commonFunctionFacade = commonFunctionFacade;
    }

    @Override
    public void contribute(String runnerScope, Map<String, Object> context) {
        context.putIfAbsent("qlCommonFns", new QlexpressFunctionFacadeAdapter()
                .bind("hasText", args -> commonFunctionFacade.hasText(valueAt(args, 0)))
                .bind("matchesRegex", args -> commonFunctionFacade.matchesRegex(valueAt(args, 0), valueAt(args, 1))));
    }

    private Object valueAt(Object[] args, int index) {
        return args == null || index < 0 || index >= args.length ? null : args[index];
    }
}
