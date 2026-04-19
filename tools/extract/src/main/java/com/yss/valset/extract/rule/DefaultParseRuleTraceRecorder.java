package com.yss.valset.extract.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.rule.ParseRuleTraceRecord;
import com.yss.valset.domain.rule.ParseRuleTraceRecorder;
import com.yss.valset.extract.repository.entity.ParseRuleTracePO;
import com.yss.valset.extract.repository.mapper.ParseRuleTraceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 默认解析规则追踪记录器。
 */
@Slf4j
@Component
public class DefaultParseRuleTraceRecorder implements ParseRuleTraceRecorder {

    private static final int MAX_TRACE_TEXT_LENGTH = 4000;

    private final ParseRuleTraceRepository traceRepository;
    private final ObjectMapper objectMapper;

    public DefaultParseRuleTraceRecorder(ParseRuleTraceRepository traceRepository,
                                         ObjectMapper objectMapper) {
        this.traceRepository = traceRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(ParseRuleTraceRecord record) {
        if (record == null) {
            return;
        }
        try {
            ParseRuleTracePO po = new ParseRuleTracePO();
            po.setTraceScope(record.getTraceScope());
            po.setTraceType(record.getTraceType());
            po.setProfileId(record.getProfileId());
            po.setProfileCode(record.getProfileCode());
            po.setVersion(record.getVersion());
            po.setFileId(record.getFileId());
            po.setTaskId(record.getTaskId());
            po.setStepName(record.getStepName());
            po.setExpression(truncate(record.getExpression()));
            po.setInputJson(truncate(record.getInputJson()));
            po.setOutputJson(truncate(record.getOutputJson()));
            po.setSuccess(record.getSuccess());
            po.setCostMs(record.getCostMs());
            po.setErrorMessage(truncate(record.getErrorMessage()));
            po.setTraceTime(record.getTraceTime() == null ? LocalDateTime.now() : record.getTraceTime());
            traceRepository.insert(po);
        } catch (Exception exception) {
            log.warn("保存解析规则追踪失败，traceType={}, stepName={}", record.getTraceType(), record.getStepName(), exception);
        }
    }

    private String truncate(String text) {
        if (text == null || text.length() <= MAX_TRACE_TEXT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_TRACE_TEXT_LENGTH);
    }
}
