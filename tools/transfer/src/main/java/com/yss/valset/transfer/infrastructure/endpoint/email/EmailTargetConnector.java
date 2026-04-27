package com.yss.valset.transfer.infrastructure.endpoint.email;

import com.yss.valset.transfer.application.port.TargetConnector;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferContext;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.domain.model.config.EmailTargetConfig;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * 邮件投递连接器。
 */
@Component
public class EmailTargetConnector implements TargetConnector {

    @Override
    public String type() {
        return TargetType.EMAIL.name();
    }

    @Override
    public boolean supports(TransferTarget target) {
        return target != null && target.targetType() == TargetType.EMAIL;
    }

    @Override
    public TransferResult send(TransferContext context) {
        EmailTargetConfig config = EmailTargetConfig.from(context);
        TransferObject transferObject = context.transferObject();
        File attachment = new File(transferObject.localTempPath());
        if (!attachment.isFile()) {
            return new TransferResult(false, null, Collections.singletonList("未找到待投递文件: " + transferObject.localTempPath()));
        }

        JavaMailSenderImpl mailSender = buildSender(config);
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(config.from());
            helper.setTo(splitAddresses(config.to()));
            if (!config.cc().isBlank()) {
                helper.setCc(splitAddresses(config.cc()));
            }
            if (!config.bcc().isBlank()) {
                helper.setBcc(splitAddresses(config.bcc()));
            }
            helper.setSubject(resolveSubject(config, transferObject));
            helper.setText(resolveBody(config, transferObject), false);
            if (config.forwardOriginalSender() && transferObject.mailFrom() != null && !transferObject.mailFrom().isBlank()) {
                helper.setReplyTo(firstNonBlank(transferObject.mailFrom(), config.from()));
            }
            helper.addAttachment(firstNonBlank(transferObject.originalName(), "transfer-file"), attachment);
            mailSender.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("邮件投递失败", e);
        }

        List<String> messages = new ArrayList<>();
        messages.add("邮件投递成功");
        messages.add("to=" + config.to());
        return new TransferResult(true, null, messages);
    }

    private JavaMailSenderImpl buildSender(EmailTargetConfig config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.host());
        sender.setPort(config.port());
        sender.setUsername(config.username());
        sender.setPassword(config.password());
        sender.setProtocol(config.protocol());
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", String.valueOf(config.auth()));
        properties.put("mail.smtp.starttls.enable", String.valueOf(config.startTls()));
        properties.put("mail.smtp.ssl.enable", String.valueOf(config.ssl()));
        properties.put("mail.smtp.connectiontimeout", String.valueOf(config.timeoutMillis()));
        properties.put("mail.smtp.timeout", String.valueOf(config.timeoutMillis()));
        properties.put("mail.smtp.writetimeout", String.valueOf(config.timeoutMillis()));
        return sender;
    }

    private String resolveSubject(EmailTargetConfig config, TransferObject transferObject) {
        if (config.subjectTemplate() != null && !config.subjectTemplate().isBlank()) {
            return renderTemplate(config.subjectTemplate(), transferObject);
        }
        String originalSubject = transferObject.mailSubject();
        if (originalSubject != null && !originalSubject.isBlank()) {
            return "邮件转发：" + originalSubject;
        }
        return "文件转发：" + firstNonBlank(transferObject.originalName(), "transfer-file");
    }

    private String resolveBody(EmailTargetConfig config, TransferObject transferObject) {
        String templateBody = null;
        if (config.bodyTemplate() != null && !config.bodyTemplate().isBlank()) {
            templateBody = renderTemplate(config.bodyTemplate(), transferObject);
        }
        if (!config.forwardMailContent()) {
            return templateBody != null ? templateBody : "文件已转发，请查收附件。";
        }
        String forwardedMail = buildForwardedMailBody(transferObject);
        if (templateBody == null || templateBody.isBlank()) {
            return forwardedMail;
        }
        return templateBody + System.lineSeparator() + System.lineSeparator() + forwardedMail;
    }

    private String renderTemplate(String template, TransferObject transferObject) {
        return template
                .replace("${fileName}", firstNonBlank(transferObject.originalName(), "transfer-file"))
                .replace("${normalizedName}", firstNonBlank(transferObject.originalName(), "transfer-file"))
                .replace("${mailId}", firstNonBlank(transferObject.mailId(), ""))
                .replace("${mailFrom}", firstNonBlank(transferObject.mailFrom(), ""))
                .replace("${mailTo}", firstNonBlank(transferObject.mailTo(), ""))
                .replace("${mailCc}", firstNonBlank(transferObject.mailCc(), ""))
                .replace("${mailBcc}", firstNonBlank(transferObject.mailBcc(), ""))
                .replace("${mailSubject}", firstNonBlank(transferObject.mailSubject(), ""))
                .replace("${mailBody}", firstNonBlank(transferObject.mailBody(), ""))
                .replace("${mailProtocol}", firstNonBlank(transferObject.mailProtocol(), ""))
                .replace("${mailFolder}", firstNonBlank(transferObject.mailFolder(), ""));
    }

    private String buildForwardedMailBody(TransferObject transferObject) {
        StringBuilder builder = new StringBuilder();
        builder.append("原始邮件信息").append(System.lineSeparator());
        appendLine(builder, "邮件ID", transferObject.mailId());
        appendLine(builder, "发件人", transferObject.mailFrom());
        appendLine(builder, "收件人", transferObject.mailTo());
        appendLine(builder, "抄送", transferObject.mailCc());
        appendLine(builder, "密送", transferObject.mailBcc());
        appendLine(builder, "主题", transferObject.mailSubject());
        appendLine(builder, "协议", transferObject.mailProtocol());
        appendLine(builder, "文件夹", transferObject.mailFolder());
        builder.append(System.lineSeparator()).append("原始正文").append(System.lineSeparator());
        builder.append(firstNonBlank(transferObject.mailBody(), ""))
                .append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("转发附件已一并附上，请查收。");
        return builder.toString();
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        builder.append(label).append("：").append(firstNonBlank(value, "")).append(System.lineSeparator());
    }

    private String[] splitAddresses(String addresses) {
        return java.util.Arrays.stream(addresses.split("[,;\\s]+"))
                .filter(value -> value != null && !value.isBlank())
                .toArray(String[]::new);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

}
