package com.yss.valset.transfer.infrastructure.source.email;

import com.yss.valset.transfer.application.service.TransferIngestProgressAppService;
import com.yss.valset.transfer.domain.gateway.TransferSourceCheckpointGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.TransferSourceCheckpoint;
import com.yss.valset.transfer.domain.model.config.EmailSourceConfig;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.infrastructure.source.support.SourceFetchLogSupport;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Comparator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 邮件扫描器，负责打开邮箱、分批遍历邮件、推进游标，并把命中的附件交给处理器解析。
 */
@Component
@RequiredArgsConstructor
public class EmailMailScanner {

    private static final Logger log = LoggerFactory.getLogger(EmailMailScanner.class);
    private static final int STOP_CHECK_INTERVAL = 10;

    private final TransferSourceCheckpointGateway transferSourceCheckpointGateway;
    private final TransferSourceGateway transferSourceGateway;
    private final EmailAttachmentProcessor emailAttachmentProcessor;
    private final TransferIngestProgressAppService transferIngestProgressAppService;

    /**
     * 邮件扫描主流程：建立会话、打开文件夹、按批次扫描邮件并生成识别上下文。
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
                int messageCount = folder.getMessageCount();
                SourceFetchLogSupport.logStart(log, "邮件", source, "folder", config.folder(), "邮件总数", messageCount);
                log.info("邮件扫描采用分批模式，sourceId={}，sourceCode={}，扫描批次={}",
                        source == null ? null : source.sourceId(),
                        source == null ? null : source.sourceCode(),
                        config.effectiveScanBatchSize());
                transferIngestProgressAppService.publishMessage(source.sourceId(),
                        "邮件收取开始，folder=" + config.folder() + "，邮件总数=" + messageCount + "，扫描批次=" + config.effectiveScanBatchSize());
                result.addAll(scanMailFolder(source, config, folder, uidFolder, messageCount, mailTimeLowerBound));
            }
        } catch (Exception e) {
            throw new IllegalStateException("收取邮件附件失败，folder=" + config.folder() + ", protocol=" + config.protocol(), e);
        }
        return result;
    }

    /**
     * 扫描文件夹中的邮件，遇到游标、去重、时间范围和停止信号时按规则提前结束。
     */
    List<RecognitionContext> scanMailFolder(
            TransferSource source,
            EmailSourceConfig config,
            Folder folder,
            UIDFolder uidFolder,
            int messageCount,
            Instant mailTimeLowerBound) throws Exception {
        List<RecognitionContext> result = new ArrayList<>();
        if (folder == null || messageCount <= 0) {
            return result;
        }

        if (config.isPop3LargeMailbox(messageCount)) {
            String pop3Notice = buildPop3LargeMailboxNotice(source, messageCount, config.pop3LargeMailboxThreshold());
            log.warn(pop3Notice);
            transferIngestProgressAppService.publishMessage(source.sourceId(), pop3Notice);
        }

        CursorState cursorState = readCursorState(source);
        Set<String> processedInThisRun = new LinkedHashSet<>();
        int limit = config.effectiveLimit();
        int batchSize = config.effectiveScanBatchSize();
        int emailSequence = 0;
        boolean seenCursor = cursorState.uid() != null || cursorState.mailId() == null || cursorState.mailId().isBlank();

        if (uidFolder != null && cursorState.uid() != null && cursorState.uid() > 0) {
            long lastUid = Math.max(0L, uidFolder.getUIDNext() - 1);
            long startUid = cursorState.uid() + 1;
            if (lastUid <= 0 || startUid > lastUid) {
                return result;
            }
            for (long batchStart = startUid; batchStart <= lastUid; batchStart += batchSize) {
                if (processedInThisRun.size() >= limit || shouldStop(source)) {
                    break;
                }
                long batchEnd = Math.min(lastUid, batchStart + batchSize - 1L);
                Message[] messages = uidFolder.getMessagesByUID(batchStart, batchEnd);
                ScanBatchResult batchResult = scanMessages(source, config, messages, uidFolder, cursorState.mailId(), mailTimeLowerBound, emailSequence, true, processedInThisRun);
                result.addAll(batchResult.contexts());
                emailSequence = batchResult.emailSequence();
                if (batchResult.latestCursorReference() != null) {
                    recordScanCursor(source, config, batchResult.latestCursorReference());
                }
                if (!batchResult.seenCursor() || batchResult.stopped() || processedInThisRun.size() >= limit) {
                    break;
                }
            }
            return result;
        }

        if (shouldUseServerSideTimeSearch(config, uidFolder, cursorState, mailTimeLowerBound)) {
            Message[] candidateMessages = loadCandidateMessagesByTimeSearch(folder, mailTimeLowerBound);
            if (candidateMessages.length == 0) {
                return result;
            }
            ScanBatchResult searchBatchResult = scanMessages(source, config, candidateMessages, uidFolder, cursorState.mailId(), mailTimeLowerBound, emailSequence, seenCursor, processedInThisRun);
            result.addAll(searchBatchResult.contexts());
            if (searchBatchResult.latestCursorReference() != null) {
                recordScanCursor(source, config, searchBatchResult.latestCursorReference());
            }
            return result;
        }

        int startMessageNumber = resolveStartMessageNumber(cursorState);
        for (int batchStart = startMessageNumber; batchStart <= messageCount; batchStart += batchSize) {
            if (processedInThisRun.size() >= limit || shouldStop(source)) {
                break;
            }
            int batchEnd = Math.min(messageCount, batchStart + batchSize - 1);
            Message[] messages = folder.getMessages(batchStart, batchEnd);
            ScanBatchResult batchResult = scanMessages(source, config, messages, uidFolder, cursorState.mailId(), mailTimeLowerBound, emailSequence, seenCursor, processedInThisRun);
            result.addAll(batchResult.contexts());
            emailSequence = batchResult.emailSequence();
            seenCursor = batchResult.seenCursor();
            if (batchResult.latestCursorReference() != null) {
                recordScanCursor(source, config, batchResult.latestCursorReference());
            }
            if (batchResult.stopped() || processedInThisRun.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private boolean shouldUseServerSideTimeSearch(EmailSourceConfig config, UIDFolder uidFolder, CursorState cursorState, Instant mailTimeLowerBound) {
        return config != null
                && uidFolder != null
                && mailTimeLowerBound != null
                && config.protocol() != null
                && config.protocol().startsWith("imap")
                && cursorState != null
                && cursorState.uid() == null
                && cursorState.messageNumber() == null
                && (cursorState.mailId() == null || cursorState.mailId().isBlank());
    }

    private Message[] loadCandidateMessagesByTimeSearch(Folder folder, Instant mailTimeLowerBound) throws Exception {
        if (folder == null || mailTimeLowerBound == null) {
            return new Message[0];
        }
        SearchTerm searchTerm = new ReceivedDateTerm(ComparisonTerm.GE, Date.from(mailTimeLowerBound));
        Message[] messages = folder.search(searchTerm);
        if (messages == null || messages.length == 0) {
            return new Message[0];
        }
        Arrays.sort(messages, Comparator.comparingInt(Message::getMessageNumber));
        return messages;
    }

    /**
     * 扫描一批邮件，按游标、时间范围、重复处理和停止信号进行过滤。
     */
    private ScanBatchResult scanMessages(
            TransferSource source,
            EmailSourceConfig config,
            Message[] messages,
            UIDFolder uidFolder,
            String cursorMailId,
            Instant mailTimeLowerBound,
            int emailSequenceStart,
            boolean seenCursorStart,
            Set<String> processedInThisRun) throws Exception {
        List<RecognitionContext> batchContexts = new ArrayList<>();
        boolean seenCursor = seenCursorStart;
        int emailSequence = emailSequenceStart;
        MailScanReference latestCursorReference = null;
        int stopCheckCounter = 0;

        if (messages == null || messages.length == 0) {
            return new ScanBatchResult(batchContexts, emailSequence, seenCursor, latestCursorReference, false);
        }

        for (Message message : messages) {
            if (processedInThisRun.size() >= config.effectiveLimit()) {
                break;
            }
            if (stopCheckCounter % STOP_CHECK_INTERVAL == 0 && shouldStop(source)) {
                log.info("邮件收取收到停止请求，提前结束，sourceId={}，sourceCode={}", source.sourceId(), source.sourceCode());
                return new ScanBatchResult(batchContexts, emailSequence, seenCursor, latestCursorReference, true);
            }
            stopCheckCounter++;

            MailScanReference currentReference = resolveMailReference(message, config, uidFolder);
            if (!seenCursor) {
                if (cursorMailId != null && cursorMailId.equals(currentReference.mailId())) {
                    seenCursor = true;
                    latestCursorReference = currentReference.withReason("游标");
                }
                continue;
            }

            if (!isWithinMailTimeRange(message, mailTimeLowerBound)) {
                log.info("邮件超出收取时间范围，跳过处理，mailId={}，主题={}，收取范围=近{}天",
                        currentReference.mailId(),
                        safeSubject(message),
                        config.mailTimeRangeDays());
                transferIngestProgressAppService.publishMessage(source.sourceId(),
                        "邮件超出收取时间范围，跳过处理，mailId=" + currentReference.mailId() + "，主题=" + safeSubject(message) + "，收取范围=近" + config.mailTimeRangeDays() + "天");
                latestCursorReference = currentReference.withReason("超出收取时间范围");
                continue;
            }

            emailSequence++;
            transferIngestProgressAppService.publishMessage(source.sourceId(),
                    "收取邮件第" + emailSequence + "封，mailId=" + currentReference.mailId() + "，主题=" + safeSubject(message));
            MailProcessingResult processingResult = processSingleMail(
                    source,
                    config,
                    message,
                    currentReference.mailId(),
                    currentReference.mailUid(),
                    currentReference.messageNumber(),
                    emailSequence,
                    processedInThisRun);
            batchContexts.addAll(processingResult.contexts());
            latestCursorReference = currentReference.withEmailSequence(emailSequence).withReason(processingResult.reason());
        }
        return new ScanBatchResult(batchContexts, emailSequence, seenCursor, latestCursorReference, false);
    }

    /**
     * 处理单封邮件，负责去重、附件提取和扫描结果归档。
     */
    private MailProcessingResult processSingleMail(
            TransferSource source,
            EmailSourceConfig config,
            Message message,
            String mailId,
            Long mailUid,
            Integer messageNumber,
            int emailSequence,
            Set<String> processedInThisRun) throws Exception {
        String subject = safeSubject(message);
        if (source.sourceId() != null && transferSourceCheckpointGateway.existsProcessedItem(source.sourceId(), mailId)) {
            log.info("收取邮件第{}封已处理过，mailId={}，主题={}", emailSequence, mailId, subject);
            return new MailProcessingResult(List.of(), "已处理");
        }
        log.info("收取邮件第{}封，mailId={}，主题={}", emailSequence, mailId, subject);
        processedInThisRun.add(mailId);
        EmailAttachmentProcessor.AttachmentExtractionResult extractionResult = emailAttachmentProcessor.extract(
                source,
                config,
                message,
                mailId,
                mailUid,
                messageNumber,
                emailSequence);
        String reason;
        if (extractionResult.contexts().isEmpty()) {
            reason = extractionResult.attachmentCount() > 0 ? "无符合条件附件" : "无附件";
        } else {
            reason = "已接收";
        }
        log.info("邮件收取完成，第{}封，mailId={}，主题={}，附件数={}", emailSequence, mailId, subject, extractionResult.acceptedCount());
        transferIngestProgressAppService.publishMessage(source.sourceId(),
                "邮件收取完成，第" + emailSequence + "封，mailId=" + mailId + "，主题=" + subject + "，附件数=" + extractionResult.acceptedCount());
        return new MailProcessingResult(extractionResult.contexts(), reason);
    }

    /**
     * 读取最近一次扫描游标，保证重复任务可以从上次位置继续。
     */
    private CursorState readCursorState(TransferSource source) {
        if (source == null || source.sourceId() == null) {
            return new CursorState(null, null, null);
        }
        return transferSourceCheckpointGateway.findCheckpoint(source.sourceId(), TransferConfigKeys.CHECKPOINT_SCAN_CURSOR)
                .map(this::toCursorState)
                .orElseGet(() -> new CursorState(null, null, null));
    }

    /**
     * 记录扫描游标，用于下次任务从当前邮件继续扫描。
     */
    private void recordScanCursor(TransferSource source, EmailSourceConfig config, MailScanReference reference) {
        if (source == null || source.sourceId() == null || reference == null || reference.mailId() == null || reference.mailId().isBlank()) {
            return;
        }
        Map<String, Object> checkpointMeta = new LinkedHashMap<>();
        checkpointMeta.put(TransferConfigKeys.SOURCE_TYPE, source.sourceType() == null ? null : source.sourceType().name());
        checkpointMeta.put(TransferConfigKeys.SOURCE_CODE, source.sourceCode());
        checkpointMeta.put(TransferConfigKeys.TRIGGER_TYPE, "FETCH");
        checkpointMeta.put(TransferConfigKeys.MAIL_ID, reference.mailId());
        checkpointMeta.put(TransferConfigKeys.MAIL_UID, reference.mailUid());
        checkpointMeta.put(TransferConfigKeys.MAIL_MESSAGE_NUMBER, reference.messageNumber());
        checkpointMeta.put(TransferConfigKeys.MAIL_SUBJECT, reference.subject());
        checkpointMeta.put(TransferConfigKeys.MAIL_FOLDER, config.folder());
        checkpointMeta.put(TransferConfigKeys.MAIL_PROTOCOL, config.protocol());
        checkpointMeta.put("emailSequence", reference.emailSequence());
        checkpointMeta.put("scanReason", reference.reason());
        checkpointMeta.put("processedAt", Instant.now().toString());
        transferSourceCheckpointGateway.saveCheckpoint(new TransferSourceCheckpoint(
                null,
                source.sourceId(),
                source.sourceType() == null ? null : source.sourceType().name(),
                TransferConfigKeys.CHECKPOINT_SCAN_CURSOR,
                reference.mailId(),
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
    private MailScanReference resolveMailReference(Message message, EmailSourceConfig config, UIDFolder uidFolder) throws Exception {
        String mailId = resolveMailId(message, config, uidFolder);
        Long mailUid = resolveMailUid(message, uidFolder);
        Integer messageNumber = message == null ? null : message.getMessageNumber();
        String subject = safeSubject(message);
        return new MailScanReference(mailId, mailUid, messageNumber, 0, subject, null);
    }

    private Long resolveMailUid(Message message, UIDFolder uidFolder) throws Exception {
        if (message == null || uidFolder == null) {
            return null;
        }
        long uid = uidFolder.getUID(message);
        return uid > 0 ? uid : null;
    }

    /**
     * 优先使用 Message-ID 作为邮件标识，缺失时回退到 UID 或消息序号。
     */
    private String resolveMailId(Message message, EmailSourceConfig config, UIDFolder uidFolder) throws Exception {
        String[] header = message.getHeader("Message-ID");
        if (header != null && header.length > 0 && header[0] != null && !header[0].isBlank()) {
            return header[0];
        }
        Long uid = resolveMailUid(message, uidFolder);
        if (uid != null) {
            return config.protocol() + ":" + config.folder() + ":" + uid;
        }
        return config.protocol() + ":" + config.folder() + ":" + message.getMessageNumber();
    }

    private int resolveStartMessageNumber(CursorState cursorState) {
        if (cursorState == null) {
            return 1;
        }
        if (cursorState.messageNumber() != null && cursorState.messageNumber() > 0) {
            return cursorState.messageNumber() + 1;
        }
        if (cursorState.mailId() != null) {
            Long parsed = parseTrailingLong(cursorState.mailId());
            if (parsed != null && parsed > 0) {
                long next = parsed + 1L;
                return next > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) next;
            }
        }
        return 1;
    }

    private CursorState toCursorState(TransferSourceCheckpoint checkpoint) {
        if (checkpoint == null) {
            return new CursorState(null, null, null);
        }
        Map<String, Object> checkpointMeta = checkpoint.checkpointMeta() == null ? Map.of() : checkpoint.checkpointMeta();
        String mailId = checkpoint.checkpointValue();
        Long mailUid = parseLongValue(checkpointMeta.get(TransferConfigKeys.MAIL_UID));
        if (mailUid == null) {
            mailUid = parseTrailingLong(mailId);
        }
        Integer messageNumber = parseIntegerValue(checkpointMeta.get(TransferConfigKeys.MAIL_MESSAGE_NUMBER));
        if (messageNumber == null) {
            Long parsedMessageNumber = parseTrailingLong(mailId);
            if (parsedMessageNumber != null && parsedMessageNumber <= Integer.MAX_VALUE) {
                messageNumber = parsedMessageNumber.intValue();
            }
        }
        return new CursorState(mailId, mailUid, messageNumber);
    }

    private Long parseTrailingLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.contains("@")) {
            return null;
        }
        int lastColon = value.lastIndexOf(':');
        String tail = lastColon >= 0 ? value.substring(lastColon + 1) : value;
        try {
            return Long.parseLong(tail);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long parseLongValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer parseIntegerValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String buildPop3LargeMailboxNotice(TransferSource source, int messageCount, int threshold) {
        long overLimit = threshold <= 0 ? messageCount : Math.max(0L, (long) messageCount - threshold);
        String suggestion = "建议切换 IMAP，或缩小 mailTimeRangeDays / 降低扫描范围";
        return "POP3 邮箱总量较大，当前仍按降级模式扫描"
                + "，sourceId=" + (source == null ? null : source.sourceId())
                + "，sourceCode=" + (source == null ? null : source.sourceCode())
                + "，邮件总数=" + messageCount
                + "，阈值=" + threshold
                + "，超出=" + overLimit
                + "，" + suggestion;
    }

    private record CursorState(String mailId, Long uid, Integer messageNumber) {
    }

    private record MailScanReference(String mailId, Long mailUid, Integer messageNumber, int emailSequence, String subject, String reason) {
        MailScanReference withEmailSequence(int emailSequence) {
            return new MailScanReference(mailId, mailUid, messageNumber, emailSequence, subject, reason);
        }

        MailScanReference withReason(String reason) {
            return new MailScanReference(mailId, mailUid, messageNumber, emailSequence, subject, reason);
        }
    }

    private record MailProcessingResult(List<RecognitionContext> contexts, String reason) {
    }

    private record ScanBatchResult(List<RecognitionContext> contexts, int emailSequence, boolean seenCursor,
                                   MailScanReference latestCursorReference, boolean stopped) {
    }
}
