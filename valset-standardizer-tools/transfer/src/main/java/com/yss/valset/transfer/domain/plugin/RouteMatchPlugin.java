package com.yss.valset.transfer.domain.plugin;

import com.yss.valset.transfer.domain.model.MatchResult;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.RecognitionContext;

/**
 * 路由匹配插件。
 */
public interface RouteMatchPlugin {

    String type();

    int priority();

    boolean supports(RecognitionContext context);

    MatchResult match(RecognitionContext context, ProbeResult probeResult);
}
