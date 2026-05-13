package com.yss.valset.transfer.infrastructure.plugin;

import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.plugin.TransferActionPlugin;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 文件投递动作插件注册表。
 */
@Component
public class TransferActionPluginRegistry {

    private final List<TransferActionPlugin> plugins;

    public TransferActionPluginRegistry(List<TransferActionPlugin> plugins) {
        this.plugins = List.copyOf(plugins);
    }

    public TransferActionPlugin getRequired(TransferRoute route) {
        return plugins.stream()
                .filter(plugin -> plugin.supports(route))
                .min(Comparator.comparingInt(TransferActionPlugin::priority))
                .orElseThrow(() -> new IllegalStateException("未找到文件投递动作插件"));
    }
}
