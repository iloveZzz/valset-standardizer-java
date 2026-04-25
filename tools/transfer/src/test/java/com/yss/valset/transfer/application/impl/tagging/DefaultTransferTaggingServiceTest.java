package com.yss.valset.transfer.application.impl.tagging;

import com.yss.valset.transfer.domain.gateway.TransferObjectTagGateway;
import com.yss.valset.transfer.domain.gateway.TransferTagGateway;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.RuleContext;
import com.yss.valset.transfer.domain.model.RuleEvaluationResult;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectTag;
import com.yss.valset.transfer.domain.model.TransferTagDefinition;
import com.yss.valset.transfer.domain.rule.RuleEngine;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultTransferTaggingServiceTest {

    @Test
    void shouldPreferTransferObjectLocalTempPathWhenBuildingTaggingContext() {
        TransferTagGateway transferTagGateway = mock(TransferTagGateway.class);
        TransferObjectTagGateway transferObjectTagGateway = mock(TransferObjectTagGateway.class);
        RuleEngine ruleEngine = mock(RuleEngine.class);

        DefaultTransferTaggingService service = new DefaultTransferTaggingService(
                transferTagGateway,
                transferObjectTagGateway,
                ruleEngine
        );

        TransferTagDefinition tagDefinition = new TransferTagDefinition(
                "tag-1",
                "TAG_CODE",
                "标签",
                "xlsx",
                true,
                10,
                "SCRIPT_RULE",
                "qlexpress4",
                "hasText(path)",
                null,
                Map.of("scanLimit", 10),
                Instant.now(),
                Instant.now()
        );
        when(transferTagGateway.listEnabledTags()).thenReturn(List.of(tagDefinition));
        when(ruleEngine.evaluate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new RuleEvaluationResult(true, List.of(), "规则命中"));

        RecognitionContext recognitionContext = new RecognitionContext(
                SourceType.EMAIL,
                "source-code",
                "mail.xls",
                "application/vnd.ms-excel",
                123L,
                "sender@example.com",
                "to@example.com",
                null,
                null,
                "subject",
                "body",
                "mail-id",
                "imap",
                "INBOX",
                "/var/folders/8d/temp-mail.xls",
                Map.of()
        );
        TransferObject transferObject = new TransferObject(
                "transfer-1",
                "source-1",
                "EMAIL",
                "source-code",
                "mail.xls",
                "xls",
                "application/vnd.ms-excel",
                123L,
                "fingerprint",
                "source-ref",
                "mail-id",
                "sender@example.com",
                "to@example.com",
                null,
                null,
                "subject",
                "body",
                "imap",
                "INBOX",
                "/Users/zhudaoming/.tmp/yss-transfer/outbox/2026-04-25/mail.xls",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                new ProbeResult(false, null, Map.of()),
                Map.of()
        );

        service.tag(transferObject, recognitionContext, null);

        ArgumentCaptor<RuleContext> contextCaptor = ArgumentCaptor.forClass(RuleContext.class);
        ArgumentCaptor<List<TransferObjectTag>> tagCaptor = ArgumentCaptor.forClass(List.class);
        verify(ruleEngine).evaluate(org.mockito.ArgumentMatchers.any(), contextCaptor.capture());
        verify(transferObjectTagGateway).saveAll(tagCaptor.capture());

        RuleContext captured = contextCaptor.getValue();
        assertThat(captured.recognitionContext().path()).isEqualTo(transferObject.localTempPath());
        assertThat(captured.variables().get("path")).isEqualTo(transferObject.localTempPath());
        assertThat(captured.recognitionContext().fileName()).isEqualTo(recognitionContext.fileName());
        assertThat(captured.recognitionContext().sourceCode()).isEqualTo(recognitionContext.sourceCode());

        Map<String, Object> matchSnapshot = tagCaptor.getValue().get(0).matchSnapshot();
        assertThat(matchSnapshot).containsKeys("sourceType", "sourceCode", "fileName", "sender", "subject", "path", "probeDetected", "probeDetectedType", "probeAttributesCount");
        assertThat(matchSnapshot).doesNotContainKeys("tagCode", "tagName", "tagValue", "transferId", "probeResult");
    }
}
