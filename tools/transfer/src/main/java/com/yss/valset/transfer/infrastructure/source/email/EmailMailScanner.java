package com.yss.valset.transfer.infrastructure.source.email;

import com.yss.valset.transfer.domain.gateway.TransferSourceCheckpointGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.TransferSourceCheckpoint;
import com.yss.valset.transfer.domain.model.config.EmailSourceConfig;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.application.service.TransferIngestProgressAppService;
import com.yss.valset.transfer.infrastructure.source.support.SourceFetchLogSupport;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import lombok.RequiredArgsConstructor;

/**
 * 邮件扫描器，负责打开邮箱、遍历邮件、推进游标，并把命中的附件交给处理器解析。
 */
@Component
@RequiredArgsConstructor
public class EmailMailScanner {

    private static final Logger log = LoggerFactory.getLogger(EmailMailScanner.class);

    private final TransferSourceCheckpointGateway transferSourceCheckpointGateway;
    private final TransferSourceGateway transferSourceGateway;
    private final EmailAttachmentProcessor emailAttachmentProcessor;
    private final TransferIngestProgressAppService transferIngestProgressAppService;

    /**
     * 邮件扫描主流程：建立会话、打开文件夹、按游标顺序扫描邮件并生成识别上下文。
     */
    public List<RecognitionContext> fetch(TransferSource source) {
        EmailSourceConfig config = EmailSourceConfig.from(source);
        Properties properties = emailAttachmentProcessor.buildMailSessionProperties(config);
        Instant mailTimeLowerBound = resolveMailTimeLowerBound(config, Instant.now());

        List<RecognitionContext> result = new ArrayList<>();
        Session session = Session.getInstance(properties);
        try (Store store = session.getStore(config.protocol())) {
            store.connect(config.host(), config.port(), config.username(), config.password());
            try (Folder folder = store.getFolder(config.folder())) {
                folder.open(Folder.READ_ONLY);
                UIDFolder uidFolder = folder instanceof UIDFolder ? (UIDFolder) folder : null;
            Message[] messages = folder.getMessages();
            SourceFetchLogSupport.logStart(log, "邮件", source, "folder", config.folder(), "邮件总数", messages.length);
            transferIngestProgressAppService.publishMessage(source.sourceId(),
                    "邮件收取开始，folder=" + config.folder() + "，邮件总数=" + messages.length);
            result.addAll(scanMailFolder(source, config, messages, uidFolder, mailTimeLowerBound));
            }
        } catch (Exception e) {
            throw new IllegalStateException("收取邮件附件失败，folder=" + config.folder() + ", protocol=" + config.protocol(), e);
        }
        return result;
    }

    /**
     * 扫描文件夹中的邮件，遇到游标、去重和停止信号时按规则提前结束。
     */
    private List<RecognitionContext> scanMailFolder(TransferSource source, EmailSourceConfig config, Message[] messages, UIDFolder uidFolder, Instant mailTimeLowerBound) throws Exception {
        List<RecognitionContext> result = new ArrayList<>();
        String cursor = readCursor(source);
        boolean seenCursor = cursor == null || cursor.isBlank();
        Set<String> processedInThisRun = new java.util.LinkedHashSet<>();
        int limit = config.effectiveLimit();
        int emailSequence = 0;
        for (Message message : messages) {
            if (processedInThisRun.size() >= limit) {
                break;
            }
            if (shouldStop(source)) {
                log.info("邮件收取收到停止请求，提前结束，sourceId={}，sourceCode={}", source.sourceId(), source.sourceCode());
                break;
            }
            String mailId = resolveMailId(message, config, uidFolder);
            if (!seenCursor) {
                if (cursor.equals(mailId)) {
                    seenCursor = true;
                }
                continue;
            }
            if (!isWithinMailTimeRange(message, mailTimeLowerBound)) {
                log.info("邮件超出收取时间范围，跳过处理，mailId={}，主题={}，收取范围=近{}天",
                        mailId,
                        safeSubject(message),
                        config.mailTimeRangeDays());
                transferIngestProgressAppService.publishMessage(source.sourceId(),
                        "邮件超出收取时间范围，跳过处理，mailId=" + mailId + "，主题=" + safeSubject(message) + "，收取范围=近" + config.mailTimeRangeDays() + "天");
                continue;
            }
            emailSequence++;
            transferIngestProgressAppService.publishMessage(source.sourceId(),
                    "收取邮件第" + emailSequence + "封，mailId=" + mailId + "，主题=" + safeSubject(message));
            result.addAll(processSingleMail(source, config, message, mailId, emailSequence, processedInThisRun));
        }
        return result;
    }

    /**
     * 处理单封邮件，负责去重、附件提取和扫描游标推进。
     */
    private List<RecognitionContext> processSingleMail(TransferSource source, EmailSourceConfig config, Message message, String mailId, int emailSequence, Set<String> processedInThisRun) throws Exception {
        List<RecognitionContext> result = new ArrayList<>();
        String subject = safeSubject(message);
        if (source.sourceId() != null && transferSourceCheckpointGateway.existsProcessedItem(source.sourceId(), mailId)) {
            recordScanCursor(source, config, mailId, subject, emailSequence + 1, "已处理");
            return result;
        }
        log.info("收取邮件第{}封，mailId={}，主题={}", emailSequence, mailId, subject);
        processedInThisRun.add(mailId);
        EmailAttachmentProcessor.AttachmentExtractionResult extractionResult = emailAttachmentProcessor.extract(source, config, message, mailId, emailSequence);
        result.addAll(extractionResult.contexts());
        if (extractionResult.contexts().isEmpty()) {
            String reason = extractionResult.attachmentCount() > 0 ? "无符合条件附件" : "无附件";
            recordScanCursor(source, config, mailId, subject, emailSequence, reason);
        }
        log.info("邮件收取完成，第{}封，mailId={}，主题={}，附件数={}", emailSequence, mailId, subject, extractionResult.acceptedCount());
        transferIngestProgressAppService.publishMessage(source.sourceId(),
                "邮件收取完成，第" + emailSequence + "封，mailId=" + mailId + "，主题=" + subject + "，附件数=" + extractionResult.acceptedCount());
        return result;
    }

    /**
     * 读取最近一次扫描游标，保证重复任务可以从上次位置继续。
     */
    private String readCursor(TransferSource source) {
        if (source == null || source.sourceId() == null) {
            return null;
        }
        return transferSourceCheckpointGateway.findCheckpoint(source.sourceId(), TransferConfigKeys.CHECKPOINT_SCAN_CURSOR)
                .map(TransferSourceCheckpoint::checkpointValue)
                .filter(value -> value != null && !value.isBlank())
                .orElse(null);
    }

    /**
     * 记录扫描游标，用于下次任务从当前邮件继续扫描。
     */
    private void recordScanCursor(TransferSource source, EmailSourceConfig config, String mailId, String subject, int emailSequence, String reason) {
        if (source == null || source.sourceId() == null || mailId == null || mailId.isBlank()) {
            return;
        }
        Map<String, Object> checkpointMeta = new LinkedHashMap<>();
        checkpointMeta.put(TransferConfigKeys.SOURCE_TYPE, source.sourceType() == null ? null : source.sourceType().name());
        checkpointMeta.put(TransferConfigKeys.SOURCE_CODE, source.sourceCode());
        checkpointMeta.put(TransferConfigKeys.TRIGGER_TYPE, "FETCH");
        checkpointMeta.put(TransferConfigKeys.MAIL_ID, mailId);
        checkpointMeta.put(TransferConfigKeys.MAIL_SUBJECT, subject);
        checkpointMeta.put(TransferConfigKeys.MAIL_FOLDER, config.folder());
        checkpointMeta.put(TransferConfigKeys.MAIL_PROTOCOL, config.protocol());
        checkpointMeta.put("emailSequence", emailSequence);
        checkpointMeta.put("scanReason", reason);
        checkpointMeta.put("processedAt", Instant.now().toString());
        transferSourceCheckpointGateway.saveCheckpoint(new TransferSourceCheckpoint(
                null,
                source.sourceId(),
                source.sourceType() == null ? null : source.sourceType().name(),
                TransferConfigKeys.CHECKPOINT_SCAN_CURSOR,
                mailId,
                checkpointMeta,
                Instant.now(),
                Instant.now()
        ));
    }

    /**
     * 检查当前来源是否已被外部停止，避免继续扫描无意义邮件。
     */
    private boolean shouldStop(TransferSource source) {
        if (source == null || source.sourceId() == null) {
            return false;
        }
        return transferSourceGateway.findById(source.sourceId())
                .map(current -> "STOPPING".equalsIgnoreCase(current.ingestStatus())
                        || "STOPPED".equalsIgnoreCase(current.ingestStatus()))
                .orElse(false);
    }

    /**
     * 读取邮件主题，失败时返回空值，避免单封邮件异常影响整批扫描。
     */
    private String safeSubject(Message message) {
        if (message == null) {
            return null;
        }
        try {
            return message.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 计算邮件收取时间范围的下界，0 表示不过滤。
     */
    Instant resolveMailTimeLowerBound(EmailSourceConfig config, Instant now) {
        if (config == null || !config.shouldApplyMailTimeRange() || now == null) {
            return null;
        }
        return now.minus(config.mailTimeRangeDays(), ChronoUnit.DAYS);
    }

    /**
     * 判断邮件接收时间是否落在允许的收取范围内。
     */
    boolean isWithinMailTimeRange(Message message, Instant mailTimeLowerBound) {
        if (mailTimeLowerBound == null || message == null) {
            return true;
        }
        Instant mailTime = resolveMailTime(message);
        if (mailTime == null) {
            return true;
        }
        return !mailTime.isBefore(mailTimeLowerBound);
    }

    private Instant resolveMailTime(Message message) {
        if (message == null) {
            return null;
        }
        try {
            java.util.Date receivedDate = message.getReceivedDate();
            if (receivedDate != null) {
                return receivedDate.toInstant();
            }
        } catch (Exception ignored) {
        }
        try {
            java.util.Date sentDate = message.getSentDate();
            if (sentDate != null) {
                return sentDate.toInstant();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 优先使用 Message-ID 作为邮件标识，缺失时回退到 UID 或消息序号。
     */
    private String resolveMailId(Message message, EmailSourceConfig config, UIDFolder uidFolder) throws Exception {
        String[] header = message.getHeader("Message-ID");
        if (header != null && header.length > 0 && header[0] != null && !header[0].isBlank()) {
            return header[0];
        }
        if (uidFolder != null) {
            long uid = uidFolder.getUID(message);
            if (uid > 0) {
                return config.protocol() + ":" + config.folder() + ":" + uid;
            }
        }
        return config.protocol() + ":" + config.folder() + ":" + message.getMessageNumber();
    }
}
