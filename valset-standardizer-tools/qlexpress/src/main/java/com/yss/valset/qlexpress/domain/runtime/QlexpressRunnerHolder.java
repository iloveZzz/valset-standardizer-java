package com.yss.valset.qlexpress.domain.runtime;

import com.alibaba.qlexpress4.Express4Runner;

/**
 * 托管 QLExpress Runner 的刷新句柄。
 */
public interface QlexpressRunnerHolder {

    Express4Runner getRunner();

    void refresh();
}
