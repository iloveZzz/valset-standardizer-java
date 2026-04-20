package com.yss.valset.transfer.infrastructure.repository;

import com.yss.valset.transfer.domain.gateway.TransferRuleGateway;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.RuleDefinition;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文件规则内存网关，提供默认演示规则。
 */
@Profile("memory")
@Repository
public class InMemoryTransferRuleGateway implements TransferRuleGateway {

    private final List<RuleDefinition> rules = List.of(
            new RuleDefinition(
                    1L,
                    "default-route",
                    "默认转发规则",
                    "1.0",
                    true,
                    1,
                    "FIRST_MATCH",
                    "qlexpress4",
                    "fn.isExcel(fileName) || fn.isCompressed(fileName) || fileName != null",
                    Instant.now().minusSeconds(3600),
                    null,
                    Map.of(
                            "targetType", TargetType.S3.name(),
                            "targetCode", "default-s3",
                            "targetPath", "/transfer/inbox",
                            "renamePattern", "${fileName}"
                    )
            )
    );

    @Override
    public List<RuleDefinition> listEnabledRules() {
        return rules.stream().filter(RuleDefinition::enabled).sorted((left, right) -> Integer.compare(left.priority(), right.priority())).toList();
    }

    @Override
    public Optional<RuleDefinition> findByRuleCode(String ruleCode) {
        return rules.stream().filter(item -> item.ruleCode().equals(ruleCode)).findFirst();
    }
}
