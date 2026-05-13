package com.yss.valset.transfer.infrastructure.plugin;

import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.plugin.FileProbePlugin;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 文件特征探测插件注册表。
 */
@Component
public class FileProbePluginRegistry {

    private final List<FileProbePlugin> plugins;

    public FileProbePluginRegistry(List<FileProbePlugin> plugins) {
        this.plugins = List.copyOf(plugins);
    }

    public FileProbePlugin getRequired(RecognitionContext context) {
        return plugins.stream()
                .filter(plugin -> plugin.supports(context))
                .min(Comparator.comparingInt(FileProbePlugin::priority))
                .orElseThrow(() -> new IllegalStateException("未找到文件特征探测插件"));
    }
}
