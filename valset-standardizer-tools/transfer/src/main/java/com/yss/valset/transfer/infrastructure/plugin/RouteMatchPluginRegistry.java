package com.yss.valset.transfer.infrastructure.plugin;

import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.plugin.RouteMatchPlugin;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 路由匹配插件注册表。
 */
@Component
public class RouteMatchPluginRegistry {

    private final List<RouteMatchPlugin> plugins;

    public RouteMatchPluginRegistry(List<RouteMatchPlugin> plugins) {
        this.plugins = List.copyOf(plugins);
    }

    public RouteMatchPlugin getRequired(RecognitionContext context) {
        return plugins.stream()
                .filter(plugin -> plugin.supports(context))
                .min(Comparator.comparingInt(RouteMatchPlugin::priority))
                .orElseThrow(() -> new IllegalStateException("未找到路由匹配插件"));
    }
}
