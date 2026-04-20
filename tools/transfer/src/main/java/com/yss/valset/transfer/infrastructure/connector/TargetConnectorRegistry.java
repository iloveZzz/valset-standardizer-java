package com.yss.valset.transfer.infrastructure.connector;

import com.yss.valset.transfer.application.port.TargetConnector;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferTarget;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 目标连接器注册表。
 */
@Component
public class TargetConnectorRegistry {

    private final Map<TargetType, TargetConnector> connectorMap;

    public TargetConnectorRegistry(List<TargetConnector> connectors) {
        this.connectorMap = connectors.stream()
                .collect(Collectors.toMap(
                        connector -> TargetType.valueOf(connector.type()),
                        connector -> connector,
                        (left, right) -> left,
                        () -> new EnumMap<>(TargetType.class)
                ));
    }

    public TargetConnector getRequired(TransferTarget target) {
        TargetConnector connector = connectorMap.get(target.targetType());
        if (connector == null) {
            throw new IllegalStateException("未找到目标连接器，targetType=" + target.targetType());
        }
        return connector;
    }
}
