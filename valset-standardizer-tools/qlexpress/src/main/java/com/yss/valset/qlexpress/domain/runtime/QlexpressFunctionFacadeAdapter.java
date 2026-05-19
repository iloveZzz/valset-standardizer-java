package com.yss.valset.qlexpress.domain.runtime;

import com.alibaba.qlexpress4.runtime.QLambda;
import com.alibaba.qlexpress4.runtime.QResult;
import com.alibaba.qlexpress4.runtime.data.DataValue;
import com.alibaba.qlexpress4.runtime.util.ValueUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将受控 Java Facade 适配为 QLExpress 可稳定调用的函数映射对象。
 */
public class QlexpressFunctionFacadeAdapter extends LinkedHashMap<String, Object> {

    public QlexpressFunctionFacadeAdapter bind(String functionName, Invocation invocation) {
        put(functionName, (QLambda) args -> new QResult(
                ValueUtils.toImmutable(new DataValue(invocation.invoke(args))),
                QResult.ResultType.RETURN
        ));
        return this;
    }

    public static QlexpressFunctionFacadeAdapter of(Map<String, Invocation> invocations) {
        QlexpressFunctionFacadeAdapter adapter = new QlexpressFunctionFacadeAdapter();
        if (invocations != null) {
            invocations.forEach(adapter::bind);
        }
        return adapter;
    }

    @FunctionalInterface
    public interface Invocation {

        Object invoke(Object[] args);
    }
}
