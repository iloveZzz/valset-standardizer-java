package com.yss.valset.extract.standardization.mapping;

import com.yss.valset.qlexpress.domain.runtime.QlexpressContextContributor;
import com.yss.valset.qlexpress.domain.runtime.QlexpressFunctionFacadeAdapter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 注入表头映射脚本允许访问的函数对象。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 110)
public class QlexpressHeaderContextContributor implements QlexpressContextContributor {

    private final QlexpressHeaderFunctionFacade headerFunctionFacade;

    public QlexpressHeaderContextContributor(QlexpressHeaderFunctionFacade headerFunctionFacade) {
        this.headerFunctionFacade = headerFunctionFacade;
    }

    @Override
    public void contribute(String runnerScope, Map<String, Object> context) {
        if ("extract.headerMapping".equalsIgnoreCase(runnerScope)) {
            context.putIfAbsent("qlHeaderFns", new QlexpressFunctionFacadeAdapter()
                    .bind("hasCandidate", args -> headerFunctionFacade.hasCandidate(valueAt(args, 0)))
                    .bind("headerContainsAnySegment", args -> headerFunctionFacade.headerContainsAnySegment(valueAt(args, 0), valueAt(args, 1)))
                    .bind("headerContainsAllSegments", args -> headerFunctionFacade.headerContainsAllSegments(valueAt(args, 0), valueAt(args, 1))));
        }
    }

    private Object valueAt(Object[] args, int index) {
        return args == null || index < 0 || index >= args.length ? null : args[index];
    }
}
