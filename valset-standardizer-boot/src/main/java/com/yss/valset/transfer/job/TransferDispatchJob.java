package com.yss.valset.transfer.job;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.transfer.application.command.IngestTransferSourceCommand;
import com.yss.valset.transfer.application.port.TransferProcessUseCase;
import com.yss.valset.transfer.domain.model.SourceType;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * 文件收发分拣 Quartz 作业。
 */
@Component
@DisallowConcurrentExecution
public class TransferDispatchJob implements Job {

    public static final String ACTION = "action";
    public static final String SOURCE_ID = "sourceId";
    public static final String SOURCE_TYPE = "sourceType";
    public static final String SOURCE_CODE = "sourceCode";
    public static final String PARAMETERS_JSON = "parametersJson";
    public static final String TRANSFER_ID = "transferId";
    public static final String ROUTE_ID = "routeId";
    public static final String RETRY_COUNT = "retryCount";

    private final TransferProcessUseCase transferProcessUseCase;
    private final ObjectMapper objectMapper;

    public TransferDispatchJob(TransferProcessUseCase transferProcessUseCase, ObjectMapper objectMapper) {
        this.transferProcessUseCase = transferProcessUseCase;
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        String action = dataMap.getString(ACTION);
        try {
            if ("INGEST".equalsIgnoreCase(action)) {
                transferProcessUseCase.ingest(buildIngestCommand(dataMap));
                return;
            }
            if ("ROUTE".equalsIgnoreCase(action)) {
                transferProcessUseCase.route(dataMap.getLong(TRANSFER_ID));
                return;
            }
            if ("DELIVER".equalsIgnoreCase(action)) {
                transferProcessUseCase.deliver(dataMap.getLong(ROUTE_ID));
                return;
            }
            throw new JobExecutionException("不支持的文件分拣动作: " + action);
        } catch (Exception exception) {
            throw new JobExecutionException("执行文件分拣作业失败", exception);
        }
    }

    private IngestTransferSourceCommand buildIngestCommand(JobDataMap dataMap) {
        Long sourceId = dataMap.getLong(SOURCE_ID);
        String sourceTypeText = dataMap.getString(SOURCE_TYPE);
        SourceType sourceType = sourceTypeText == null || sourceTypeText.isBlank() ? null : SourceType.valueOf(sourceTypeText);
        String sourceCode = dataMap.getString(SOURCE_CODE);
        Map<String, Object> parameters = parseParameters(dataMap.getString(PARAMETERS_JSON));
        return new IngestTransferSourceCommand(sourceId, sourceType, sourceCode, parameters);
    }

    private Map<String, Object> parseParameters(String parametersJson) {
        if (parametersJson == null || parametersJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(parametersJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("解析文件收取参数失败", e);
        }
    }
}
