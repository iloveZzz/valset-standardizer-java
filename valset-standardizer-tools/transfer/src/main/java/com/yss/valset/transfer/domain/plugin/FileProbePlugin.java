package com.yss.valset.transfer.domain.plugin;

import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.RecognitionContext;

/**
 * 文件特征探测插件。
 */
public interface FileProbePlugin {

    String type();

    int priority();

    boolean supports(RecognitionContext context);

    ProbeResult probe(RecognitionContext context);
}
