package com.yss.valset.transfer.application.impl.management;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.transfer.application.command.TransferTagTestCommand;
import com.yss.valset.transfer.application.command.TransferTagUpsertCommand;
import com.yss.valset.transfer.application.dto.TransferTagMutationResponse;
import com.yss.valset.transfer.application.dto.TransferTagTestResultDTO;
import com.yss.valset.transfer.application.dto.TransferTagViewDTO;
import com.yss.valset.transfer.application.service.TransferTagManagementAppService;
import com.yss.valset.transfer.application.service.TransferTaggingUseCase;
import com.yss.valset.transfer.domain.form.TransferFormTemplateNames;
import com.yss.valset.transfer.domain.gateway.TransferTagGateway;
import com.yss.valset.transfer.domain.model.TransferTagDefinition;
import com.yss.valset.transfer.domain.model.TransferTagPage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * 默认标签管理服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferTagManagementAppService implements TransferTagManagementAppService {

    private final TransferTagGateway transferTagGateway;
    private final TransferTaggingUseCase transferTaggingUseCase;

    @Override
    public PageResult<TransferTagViewDTO> pageTags(String tagCode, String matchStrategy, Boolean enabled, Integer pageIndex, Integer pageSize) {
        TransferTagPage page = transferTagGateway.pageTags(tagCode, matchStrategy, enabled, pageIndex, pageSize);
        return PageResult.of(page.records().stream().map(this::toView).toList(),
                page.total(),
                page.pageSize(),
                page.pageIndex());
    }

    @Override
    public TransferTagViewDTO getTag(String tagId) {
        return toView(transferTagGateway.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到标签，tagId=" + tagId)));
    }

    @Override
    public TransferTagMutationResponse upsertTag(TransferTagUpsertCommand command) {
        validateCommand(command);
        boolean createMode = command.getTagId() == null;
        TransferTagDefinition existing = createMode ? null : transferTagGateway.findById(command.getTagId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到标签，tagId=" + command.getTagId()));
        TransferTagDefinition definition = new TransferTagDefinition(
                command.getTagId(),
                command.getTagCode(),
                command.getTagName(),
                command.getTagValue(),
                Boolean.TRUE.equals(command.getEnabled()),
                command.getPriority() == null ? 10 : command.getPriority(),
                command.getMatchStrategy(),
                command.getScriptLanguage(),
                command.getScriptBody(),
                command.getRegexPattern(),
                command.getTagMeta(),
                createMode ? Instant.now() : existing.createdAt(),
                Instant.now()
        );
        TransferTagDefinition saved = transferTagGateway.save(definition);
        return TransferTagMutationResponse.builder()
                .operation(createMode ? "create" : "update")
                .message("标签保存成功")
                .formTemplateName(TransferFormTemplateNames.TRANSFER_TAG)
                .tag(toView(saved))
                .build();
    }

    @Override
    public TransferTagMutationResponse deleteTag(String tagId) {
        TransferTagDefinition existing = transferTagGateway.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到标签，tagId=" + tagId));
        transferTagGateway.deleteById(tagId);
        return TransferTagMutationResponse.builder()
                .operation("delete")
                .message("标签删除成功")
                .formTemplateName(TransferFormTemplateNames.TRANSFER_TAG)
                .tag(toView(existing))
                .build();
    }

    @Override
    public TransferTagTestResultDTO testTag(String tagId, TransferTagTestCommand command) {
        return transferTaggingUseCase.test(tagId, command);
    }

    private void validateCommand(TransferTagUpsertCommand command) {
        if (command == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "标签配置不能为空");
        }
        String matchStrategy = command.getMatchStrategy() == null ? "" : command.getMatchStrategy().trim().toUpperCase();
        transferTagGateway.findByTagCode(command.getTagCode())
                .ifPresent(existing -> {
                    if (command.getTagId() == null || !command.getTagId().equals(existing.tagId())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "标签编码已存在，tagCode=" + command.getTagCode());
                    }
                });
        if (matchStrategy.contains("SCRIPT") && (command.getScriptBody() == null || command.getScriptBody().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "脚本匹配策略下脚本内容不能为空");
        }
        if (matchStrategy.contains("REGEX")) {
            if (command.getRegexPattern() == null || command.getRegexPattern().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "正则匹配策略下正则表达式不能为空");
            }
            try {
                java.util.regex.Pattern.compile(command.getRegexPattern());
            } catch (Exception exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "正则表达式不合法：" + exception.getMessage(), exception);
            }
        }
    }

    private TransferTagViewDTO toView(TransferTagDefinition definition) {
        return TransferTagViewDTO.builder()
                .tagId(definition.tagId())
                .tagCode(definition.tagCode())
                .tagName(definition.tagName())
                .tagValue(definition.tagValue())
                .enabled(definition.enabled())
                .priority(definition.priority())
                .matchStrategy(definition.matchStrategy())
                .scriptLanguage(definition.scriptLanguage())
                .scriptBody(definition.scriptBody())
                .regexPattern(definition.regexPattern())
                .tagMeta(definition.tagMeta())
                .createdAt(toLocalDateTime(definition.createdAt()))
                .updatedAt(toLocalDateTime(definition.updatedAt()))
                .formTemplateName(TransferFormTemplateNames.TRANSFER_TAG)
                .build();
    }

    private Instant toInstant(java.time.LocalDateTime value) {
        return value == null ? null : value.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }

    private java.time.LocalDateTime toLocalDateTime(Instant value) {
        return value == null ? null : java.time.LocalDateTime.ofInstant(value, java.time.ZoneId.systemDefault());
    }
}
