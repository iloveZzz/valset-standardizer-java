package com.yss.valset.transfer.application.impl;

import com.yss.valset.transfer.application.command.IngestTransferSourceCommand;
import com.yss.valset.transfer.application.port.IngestTransferUseCase;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.infrastructure.connector.SourceConnectorRegistry;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 默认文件收取应用服务。
 */
@Service
public class DefaultIngestTransferService implements IngestTransferUseCase {

    private final SourceConnectorRegistry sourceConnectorRegistry;
    private final TransferObjectGateway transferObjectGateway;

    public DefaultIngestTransferService(
            SourceConnectorRegistry sourceConnectorRegistry,
            TransferObjectGateway transferObjectGateway
    ) {
        this.sourceConnectorRegistry = sourceConnectorRegistry;
        this.transferObjectGateway = transferObjectGateway;
    }

    @Override
    public void execute(IngestTransferSourceCommand command) {
        TransferSource source = new TransferSource(
                command.sourceId(),
                command.sourceCode(),
                command.sourceCode(),
                command.sourceType(),
                true,
                null,
                command.parameters(),
                Map.of(),
                Map.of()
        );
        var connector = sourceConnectorRegistry.getRequired(source);
        List<RecognitionContext> contexts = connector.fetch(source);
        for (RecognitionContext context : contexts) {
            String fingerprint = fingerprint(source, context);
            if (transferObjectGateway.findByFingerprint(fingerprint).isPresent()) {
                continue;
            }
            Map<String, Object> fileMeta = new LinkedHashMap<>();
            if (context.attributes() != null) {
                fileMeta.putAll(context.attributes());
            }
            fileMeta.putIfAbsent("sourceType", source.sourceType() == null ? null : source.sourceType().name());
            fileMeta.putIfAbsent("sourceCode", source.sourceCode());
            fileMeta.putIfAbsent("sourceRef", buildSourceRef(source, context));
            fileMeta.putIfAbsent("mailId", context.mailId());
            fileMeta.putIfAbsent("mailFrom", context.sender());
            fileMeta.putIfAbsent("mailTo", context.recipientsTo());
            fileMeta.putIfAbsent("mailCc", context.recipientsCc());
            fileMeta.putIfAbsent("mailBcc", context.recipientsBcc());
            fileMeta.putIfAbsent("mailSubject", context.subject());
            fileMeta.putIfAbsent("mailBody", context.body());
            fileMeta.putIfAbsent("mailProtocol", context.mailProtocol());
            fileMeta.putIfAbsent("mailFolder", context.mailFolder());
            TransferObject transferObject = new TransferObject(
                    null,
                    source.sourceId(),
                    context.fileName(),
                    context.fileName(),
                    extensionOf(context.fileName()),
                    context.mimeType(),
                    fileSizeOf(context),
                    fingerprint,
                    buildSourceRef(source, context),
                    context.mailId(),
                    context.sender(),
                    context.recipientsTo(),
                    context.recipientsCc(),
                    context.recipientsBcc(),
                    context.subject(),
                    context.body(),
                    context.mailProtocol(),
                    context.mailFolder(),
                    context.path(),
                    TransferStatus.RECEIVED,
                    Instant.now(),
                    Instant.now(),
                    null,
                    null,
                    fileMeta
            );
            transferObjectGateway.save(transferObject);
        }
    }

    private String buildSourceRef(TransferSource source, RecognitionContext context) {
        if (context.mailId() != null && !context.mailId().isBlank()) {
            return source.sourceType() + ":" + Objects.toString(source.sourceCode(), "") + ":" + context.mailId() + ":" + Objects.toString(context.fileName(), "");
        }
        String stableSourceRef = stableSourceRefFromAttributes(source, context);
        if (stableSourceRef != null && !stableSourceRef.isBlank()) {
            return source.sourceType() + ":" + Objects.toString(source.sourceCode(), "") + ":" + stableSourceRef;
        }
        return source.sourceType() + ":" + Objects.toString(source.sourceCode(), "") + ":" + Objects.toString(context.path(), "");
    }

    private String stableSourceRefFromAttributes(TransferSource source, RecognitionContext context) {
        if (context.attributes() != null) {
            Object remotePath = context.attributes().get("remotePath");
            if (remotePath != null && !String.valueOf(remotePath).isBlank()) {
                return String.valueOf(remotePath);
            }
            Object objectKey = context.attributes().get("objectKey");
            if (objectKey != null && !String.valueOf(objectKey).isBlank()) {
                Object bucket = context.attributes().get("bucket");
                if (bucket != null && !String.valueOf(bucket).isBlank()) {
                    return bucket + ":" + objectKey;
                }
                return String.valueOf(objectKey);
            }
        }
        if (source.sourceMeta() != null) {
            Object bucket = source.sourceMeta().get("bucket");
            Object objectKey = source.sourceMeta().get("objectKey");
            if (bucket != null && objectKey != null) {
                return bucket + ":" + objectKey;
            }
        }
        return null;
    }

    private Long fileSizeOf(RecognitionContext context) {
        return context.fileSize();
    }

    private String extensionOf(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(dotIndex + 1);
    }

    private String fingerprint(TransferSource source, RecognitionContext context) {
        String raw = String.join(
                "|",
                Objects.toString(source.sourceType(), ""),
                Objects.toString(source.sourceCode(), ""),
                Objects.toString(context.fileName(), ""),
                Objects.toString(buildSourceRef(source, context), ""),
                Objects.toString(context.mimeType(), ""),
                Objects.toString(context.fileSize(), ""),
                Objects.toString(context.sender(), ""),
                Objects.toString(context.recipientsTo(), ""),
                Objects.toString(context.recipientsCc(), ""),
                Objects.toString(context.recipientsBcc(), ""),
                Objects.toString(context.subject(), ""),
                Objects.toString(context.body(), ""),
                Objects.toString(context.mailId(), ""),
                Objects.toString(context.mailProtocol(), ""),
                Objects.toString(context.mailFolder(), "")
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("无法计算文件指纹", e);
        }
    }
}
