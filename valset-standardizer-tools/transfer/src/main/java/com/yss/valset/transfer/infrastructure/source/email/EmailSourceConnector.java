package com.yss.valset.transfer.infrastructure.source.email;

import com.yss.valset.transfer.application.port.SourceConnector;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.config.EmailSourceConfig;
import java.nio.file.Path;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 邮件来源连接器，支持通过 IMAP/IMAPS/POP3/POP3S 收取邮件附件，并保留邮件正文和头信息。
 */
@Component
@RequiredArgsConstructor
public class EmailSourceConnector implements SourceConnector {

    private final EmailMailScanner emailMailScanner;
    private final EmailAttachmentProcessor emailAttachmentProcessor;

    @Override
    public String type() {
        return SourceType.EMAIL.name();
    }

    @Override
    public boolean supports(TransferSource source) {
        return source != null && source.sourceType() == SourceType.EMAIL;
    }

    /**
     * 邮件拉取入口，具体扫描、去重和游标推进交给独立扫描器处理。
     */
    @Override
    public List<RecognitionContext> fetch(TransferSource source) {
        return emailMailScanner.fetch(source);
    }

    /**
     * 邮件附件落盘主流程：根据 mailId 反查邮件，再定位目标附件并写入临时文件。
     */
    @Override
    public Path materialize(TransferSource source, com.yss.valset.transfer.domain.model.TransferObject transferObject) {
        if (transferObject == null || transferObject.mailId() == null || transferObject.mailId().isBlank()) {
            throw new IllegalStateException("邮件附件落盘失败，缺少 mailId");
        }
        try {
            return emailAttachmentProcessor.materialize(EmailSourceConfig.from(source), transferObject);
        } catch (Exception e) {
            throw new IllegalStateException("邮件附件落盘失败，mailId=" + transferObject.mailId() + ", fileName=" + transferObject.originalName(), e);
        }
    }
}
