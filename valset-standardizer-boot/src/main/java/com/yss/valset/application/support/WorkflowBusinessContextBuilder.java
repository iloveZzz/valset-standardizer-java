package com.yss.valset.application.support;

import com.yss.valset.application.command.EvaluateMappingTaskCommand;
import com.yss.valset.application.command.MatchTaskCommand;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.application.dto.StoredFileDTO;
import com.yss.valset.application.dto.workflow.WorkflowContextKeys;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.extract.application.command.ExtractDataTaskCommand;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作流业务专属上下文构建器。
 *
 * <p>
 * 这里只放和具体业务场景强相关的参数，例如工作簿路径、文件标识、指纹、topK 等。
 * 通用执行信息需要交给 {@link WorkflowCommonContextBuilder} 处理。
 * </p>
 */
@Component
public class WorkflowBusinessContextBuilder {

    /**
     * 从已落库的文件信息中构造提取阶段的业务上下文。
     */
    public Map<String, Object> build(StoredFileDTO storedFile,
                                     ValsetFileInfo fileInfo) {
        Map<String, Object> context = new LinkedHashMap<>();
        put(context, WorkflowContextKeys.DATA_SOURCE_TYPE, text(storedFile == null ? null : storedFile.getDataSourceType()));
        put(context, WorkflowContextKeys.WORKBOOK_PATH, storedFile == null ? null : storedFile.getAbsolutePath());
        put(context, WorkflowContextKeys.FILE_FINGERPRINT, text(storedFile == null ? null : storedFile.getFileFingerprint()));
        put(context, WorkflowContextKeys.FILE_SIZE_BYTES, storedFile == null ? null : storedFile.getFileSizeBytes());
        put(context, WorkflowContextKeys.FILESYS_TASK_ID, text(storedFile == null ? null : storedFile.getFilesysTaskId()));
        put(context, WorkflowContextKeys.FILESYS_FILE_ID, text(storedFile == null ? null : storedFile.getFilesysFileId()));
        put(context, WorkflowContextKeys.FILESYS_OBJECT_KEY, text(storedFile == null ? null : storedFile.getFilesysObjectKey()));
        put(context, WorkflowContextKeys.FILESYS_INSTANT_UPLOAD, storedFile == null ? null : storedFile.getFilesysInstantUpload());
        put(context, WorkflowContextKeys.FILE_ID, fileInfo == null ? null : fileInfo.getFileId());
        put(context, WorkflowContextKeys.FILE_NAME_ORIGINAL, fileInfo == null ? null : fileInfo.getFileNameOriginal());
        put(context, WorkflowContextKeys.SOURCE_CHANNEL, fileInfo == null || fileInfo.getSourceChannel() == null ? null : fileInfo.getSourceChannel().name());
        put(context, WorkflowContextKeys.SOURCE_URI, fileInfo == null ? null : fileInfo.getSourceUri());
        put(context, WorkflowContextKeys.STORAGE_URI, fileInfo == null ? null : fileInfo.getStorageUri());
        put(context, WorkflowContextKeys.FILE_STATUS, fileInfo == null ? null : fileInfo.getFileStatus() == null ? null : fileInfo.getFileStatus().name());
        return context;
    }

    /**
     * 从解析任务参数中构造业务上下文。
     */
    public Map<String, Object> build(ParseTaskCommand command) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (command == null) {
            return context;
        }
        put(context, WorkflowContextKeys.DATA_SOURCE_TYPE, text(command.getDataSourceType()));
        put(context, WorkflowContextKeys.WORKBOOK_PATH, command.getWorkbookPath());
        put(context, WorkflowContextKeys.FILE_ID, command.getFileId());
        put(context, WorkflowContextKeys.FILE_NAME_ORIGINAL, text(command.getFileNameOriginal()));
        return context;
    }

    /**
     * 从匹配任务参数中构造业务上下文。
     */
    public Map<String, Object> build(MatchTaskCommand command) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (command == null) {
            return context;
        }
        put(context, WorkflowContextKeys.DATA_SOURCE_TYPE, text(command.getDataSourceType()));
        put(context, WorkflowContextKeys.WORKBOOK_PATH, command.getWorkbookPath());
        put(context, WorkflowContextKeys.FILE_ID, command.getFileId());
        put(context, WorkflowContextKeys.TOP_K, command.getTopK());
        return context;
    }

    /**
     * 从文件提取任务参数中构造业务上下文。
     */
    public Map<String, Object> build(ExtractDataTaskCommand command) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (command == null) {
            return context;
        }
        put(context, WorkflowContextKeys.DATA_SOURCE_TYPE, text(command.getDataSourceType()));
        put(context, WorkflowContextKeys.WORKBOOK_PATH, command.getWorkbookPath());
        put(context, WorkflowContextKeys.FILE_ID, command.getFileId());
        put(context, WorkflowContextKeys.FILE_FINGERPRINT, text(command.getFileFingerprint()));
        put(context, WorkflowContextKeys.FILESYS_TASK_ID, text(command.getFilesysTaskId()));
        put(context, WorkflowContextKeys.FILESYS_FILE_ID, text(command.getFilesysFileId()));
        put(context, WorkflowContextKeys.FILESYS_OBJECT_KEY, text(command.getFilesysObjectKey()));
        put(context, WorkflowContextKeys.FILESYS_INSTANT_UPLOAD, command.getFilesysInstantUpload());
        return context;
    }

    /**
     * 从评估任务参数中构造业务上下文。
     */
    public Map<String, Object> build(EvaluateMappingTaskCommand command) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (command == null) {
            return context;
        }
        put(context, WorkflowContextKeys.MAPPING_WORKBOOK_PATH, command.getMappingWorkbookPath());
        put(context, WorkflowContextKeys.STANDARD_WORKBOOK_PATH, command.getStandardWorkbookPath());
        put(context, WorkflowContextKeys.STANDARD_SOURCE_TYPE, text(command.getStandardSourceType()));
        put(context, WorkflowContextKeys.SPLIT_MODE, text(command.getSplitMode()));
        put(context, WorkflowContextKeys.TOP_K, command.getTopK());
        put(context, WorkflowContextKeys.MAX_TUNING_SAMPLES, command.getMaxTuningSamples());
        put(context, WorkflowContextKeys.MAX_TEST_SAMPLES, command.getMaxTestSamples());
        return context;
    }

    private void put(Map<String, Object> context, String key, Object value) {
        context.put(key, value);
    }

    private String text(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
