package com.yss.valset.transfer.application.impl.tagging;

import com.yss.valset.transfer.application.impl.query.DefaultTransferObjectQueryService;
import com.yss.valset.transfer.application.service.TransferObjectBusinessFieldProjectionUseCase;
import com.yss.valset.transfer.domain.gateway.ProductMatchRuleGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectTagGateway;
import com.yss.valset.transfer.domain.gateway.TransferTagGateway;
import com.yss.valset.transfer.domain.model.ProductMatchRule;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectAnalysis;
import com.yss.valset.transfer.domain.model.TransferObjectPage;
import com.yss.valset.transfer.domain.model.TransferObjectTag;
import com.yss.valset.transfer.domain.model.TransferObjectTrend;
import com.yss.valset.transfer.domain.model.TransferTagDefinition;
import com.yss.valset.transfer.domain.model.TransferTagPage;
import com.yss.valset.transfer.domain.rule.ScriptRuleEngineAdapter;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultTransferTaggingServiceTest {

    @Test
    void scriptBooleanResultKeepsConfiguredTagValue() {
        CapturingObjectTagGateway objectTagGateway = new CapturingObjectTagGateway();
        DefaultTransferTaggingService service = newService(
                Collections.singletonList(tag("1", "BOOLEAN_TAG", "FIXED_VALUE", "return true;")),
                Collections.<ProductMatchRule>emptyList(),
                objectTagGateway
        );

        List<TransferObjectTag> tags = service.tag(transferObject("abc.xlsx"), null, null);

        assertEquals(1, tags.size());
        assertEquals("FIXED_VALUE", objectTagGateway.saved.get(0).tagValue());
    }

    @Test
    void scriptMapResultOverridesTagValueWithProductRuleId() {
        String script = "if (!hasText(fileName)) {\n"
                + "    return false;\n"
                + "}\n"
                + "rule = firstProductMatchRule(fileName, productMatchRules);\n"
                + "if (rule == null) {\n"
                + "    return false;\n"
                + "}\n"
                + "return productMatchResult(rule, fileName);";
        ProductMatchRule rule = new ProductMatchRule(
                "26169",
                "估值文件",
                "NYADTCZQSM2419",
                "农银理财农银安心灵珑同业存单及存款增强2024年第19期私募理财产品",
                "91110101MA01LP4F9T",
                "农银理财",
                "FIM",
                null,
                "(.*)农银理财农银安心·灵珑同业存单及存款增强2024年第19期私募理财产品(.*).xlsx?",
                null,
                null
        );
        CapturingObjectTagGateway objectTagGateway = new CapturingObjectTagGateway();
        DefaultTransferTaggingService service = newService(
                Collections.singletonList(tag("2", "PRODUCT_MATCH_RULE", "PRODUCT_MATCH_RULE", script)),
                Collections.singletonList(rule),
                objectTagGateway
        );

        List<TransferObjectTag> tags = service.tag(transferObject("TA_农银理财农银安心·灵珑同业存单及存款增强2024年第19期私募理财产品_估值表.xlsx"), null, null);

        assertEquals(1, tags.size());
        TransferObjectTag tag = objectTagGateway.saved.get(0);
        assertEquals("PRODUCT_MATCH_RULE", tag.tagCode());
        assertEquals("26169", tag.tagValue());
        assertEquals("fileName", tag.matchedField());
        assertTrue(tag.matchSnapshot().containsKey("pdCd"));
        assertEquals("NYADTCZQSM2419", tag.matchSnapshot().get("pdCd"));
    }

    private DefaultTransferTaggingService newService(List<TransferTagDefinition> tagDefinitions,
                                                     List<ProductMatchRule> productMatchRules,
                                                     CapturingObjectTagGateway objectTagGateway) {
        return new DefaultTransferTaggingService(
                new EmptyObjectGateway(),
                new StaticTagGateway(tagDefinitions),
                objectTagGateway,
                new NoopProjectionService(),
                new StaticProductMatchRuleGateway(productMatchRules),
                new ScriptRuleEngineAdapter()
        );
    }

    private TransferTagDefinition tag(String tagId, String tagCode, String tagValue, String scriptBody) {
        return new TransferTagDefinition(
                tagId,
                tagCode,
                tagCode,
                tagValue,
                true,
                10,
                "SCRIPT_RULE",
                "qlexpress4",
                scriptBody,
                null,
                Collections.<String, Object>emptyMap(),
                Instant.now(),
                Instant.now()
        );
    }

    private TransferObject transferObject(String fileName) {
        return new TransferObject(
                "1001",
                "2001",
                "HTTP",
                "manual",
                fileName,
                "xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                1024L,
                "fingerprint",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                Collections.<String, Object>emptyMap(),
                null
        );
    }

    private static final class StaticProductMatchRuleGateway implements ProductMatchRuleGateway {
        private final List<ProductMatchRule> rules;

        private StaticProductMatchRuleGateway(List<ProductMatchRule> rules) {
            this.rules = rules;
        }

        @Override
        public List<ProductMatchRule> listEnabledRules() {
            return rules;
        }
    }

    private static final class StaticTagGateway implements TransferTagGateway {
        private final List<TransferTagDefinition> tags;

        private StaticTagGateway(List<TransferTagDefinition> tags) {
            this.tags = tags;
        }

        @Override
        public Optional<TransferTagDefinition> findById(String tagId) {
            return tags.stream().filter(tag -> tag.tagId().equals(tagId)).findFirst();
        }

        @Override
        public Optional<TransferTagDefinition> findByTagCode(String tagCode) {
            return Optional.empty();
        }

        @Override
        public TransferTagPage pageTags(String tagCode, String tagName, String matchStrategy, Boolean enabled, Integer pageIndex, Integer pageSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TransferTagDefinition> listEnabledTags() {
            return tags;
        }

        @Override
        public TransferTagDefinition save(TransferTagDefinition definition) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(String tagId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class CapturingObjectTagGateway implements TransferObjectTagGateway {
        private final List<TransferObjectTag> saved = new ArrayList<>();

        @Override
        public List<TransferObjectTag> listByTransferId(String transferId) {
            return Collections.emptyList();
        }

        @Override
        public List<TransferObjectTag> listByTransferIds(List<String> transferIds) {
            return Collections.emptyList();
        }

        @Override
        public List<TransferObjectTag> saveAll(List<TransferObjectTag> tags) {
            saved.addAll(tags);
            return tags;
        }

        @Override
        public void deleteByTransferId(String transferId) {
            saved.clear();
        }

        @Override
        public List<TagSummary> summarizeTags(java.time.LocalDateTime startInclusive, java.time.LocalDateTime endExclusive) {
            return Collections.emptyList();
        }
    }

    private static final class NoopProjectionService implements TransferObjectBusinessFieldProjectionUseCase {
        @Override
        public TransferObject project(TransferObject transferObject, List<TransferObjectTag> tags) {
            return transferObject;
        }
    }

    private static final class EmptyObjectGateway implements TransferObjectGateway {
        @Override
        public Optional<TransferObject> findById(String transferId) {
            return Optional.empty();
        }

        @Override
        public Optional<TransferObject> findByFingerprint(String fingerprint) {
            return Optional.empty();
        }

        @Override
        public TransferObjectPage pageObjects(String sourceId, String sourceType, String sourceCode, String originalName, String status, String deliveryStatus, String mailId, String fingerprint, String routeId, String tagId, String tagCode, String tagValue, String taskDate, Integer pageIndex, Integer pageSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TransferObjectAnalysis analyzeObjects(String sourceId, String sourceType, String sourceCode, String originalName, String status, String deliveryStatus, String mailId, String fingerprint, String routeId, String tagId, String tagCode, String tagValue, String taskDate) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TransferObjectTrend> trendObjects(String taskDate, Integer days) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TransferObject> listEmailInboxObjects(String sourceCode, String mailId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TransferObject> listParseQueueCandidates(String sourceId, String sourceCode, String routeId, String status, String deliveryStatus, Integer limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DefaultTransferObjectQueryService.InboxMailGroup> loadMailInboxGroups(String sourceCode, String mailId, String deliveryStatus, Integer offset, Integer limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countMailInboxGroups(String sourceCode, String mailId, String deliveryStatus) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TransferObject save(TransferObject transferObject) {
            return transferObject;
        }
    }
}
