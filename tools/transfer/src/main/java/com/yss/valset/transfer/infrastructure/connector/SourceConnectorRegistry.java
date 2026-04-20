package com.yss.valset.transfer.infrastructure.connector;

import com.yss.valset.transfer.application.port.SourceConnector;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 来源连接器注册表。
 */
@Component
public class SourceConnectorRegistry {

    private final Map<SourceType, SourceConnector> connectorMap;

    public SourceConnectorRegistry(List<SourceConnector> connectors) {
        this.connectorMap = connectors.stream()
                .collect(Collectors.toMap(
                        connector -> SourceType.valueOf(connector.type()),
                        connector -> connector,
                        (left, right) -> left,
                        () -> new EnumMap<>(SourceType.class)
                ));
    }

    public SourceConnector getRequired(TransferSource source) {
        SourceConnector connector = connectorMap.get(source.sourceType());
        if (connector == null) {
            throw new IllegalStateException("未找到来源连接器，sourceType=" + source.sourceType());
        }
        return connector;
    }
}
