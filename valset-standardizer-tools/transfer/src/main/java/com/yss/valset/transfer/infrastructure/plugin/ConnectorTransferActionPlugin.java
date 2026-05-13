package com.yss.valset.transfer.infrastructure.plugin;

import com.yss.valset.transfer.application.port.TargetConnector;
import com.yss.valset.transfer.domain.gateway.TransferTargetGateway;
import com.yss.valset.transfer.domain.model.TransferContext;
import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.domain.plugin.TransferActionPlugin;
import com.yss.valset.transfer.infrastructure.connector.TargetConnectorRegistry;
import org.springframework.stereotype.Component;

/**
 * 基于目标连接器的投递动作插件。
 */
@Component
public class ConnectorTransferActionPlugin implements TransferActionPlugin {

    private final TransferTargetGateway transferTargetGateway;
    private final TargetConnectorRegistry targetConnectorRegistry;

    public ConnectorTransferActionPlugin(
            TransferTargetGateway transferTargetGateway,
            TargetConnectorRegistry targetConnectorRegistry
    ) {
        this.transferTargetGateway = transferTargetGateway;
        this.targetConnectorRegistry = targetConnectorRegistry;
    }

    @Override
    public String type() {
        return "CONNECTOR";
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean supports(TransferRoute route) {
        return route != null && route.targetCode() != null && !route.targetCode().isBlank();
    }

    @Override
    public TransferResult execute(TransferContext context) {
        TransferRoute route = context.transferRoute();
        TransferTarget target = context.transferTarget();
        if (target == null) {
            target = transferTargetGateway.findByTargetCode(route.targetCode())
                    .orElseThrow(() -> new IllegalStateException("未找到投递目标，targetCode=" + route.targetCode()));
        }
        TargetConnector targetConnector = targetConnectorRegistry.getRequired(target);
        return targetConnector.send(new TransferContext(context.transferObject(), route, target, context.attributes()));
    }
}
