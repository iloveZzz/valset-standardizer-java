package com.yss.valset.workflow.springbatch;

import com.yss.cloud.sankuai.SegmentContextHelper;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 批量任务 元数据主键增量器工厂。
 *
 * <p>
 * 批量任务 默认会用数据库序列表生成 {@code BATCH_*} 主键。
 * 这里改成统一从 {@link SegmentContextHelper} 取号，避免序列表状态异常导致主键回退或冲突。
 * </p>
 */
public class SpringBatchSegmentContextIncrementerFactory implements DataFieldMaxValueIncrementerFactory {

    private static final String JOB_INSTANCE_TABLE = "BATCH_JOB_INSTANCE";

    private static final String JOB_EXECUTION_TABLE = "BATCH_JOB_EXECUTION";

    private static final String STEP_EXECUTION_TABLE = "BATCH_STEP_EXECUTION";

    private final Map<String, DataFieldMaxValueIncrementer> incrementerCache = new ConcurrentHashMap<>();

    @Override
    public DataFieldMaxValueIncrementer getIncrementer(String incrementerType, String incrementerName) {
        Assert.hasText(incrementerName, "incrementerName must not be blank");
        return incrementerCache.computeIfAbsent(incrementerName, this::createIncrementer);
    }

    @Override
    public boolean isSupportedIncrementerType(String incrementerType) {
        return true;
    }

    @Override
    public String[] getSupportedIncrementerTypes() {
        return new String[]{"segment"};
    }

    private DataFieldMaxValueIncrementer createIncrementer(String incrementerName) {
        return new SegmentContextMaxValueIncrementer(resolveTargetTableName(incrementerName));
    }

    private String resolveTargetTableName(String incrementerName) {
        String normalizedName = StringUtils.trimWhitespace(incrementerName);
        if (!StringUtils.hasText(normalizedName)) {
            throw new IllegalArgumentException("incrementerName must not be blank");
        }

        String upperName = normalizedName.toUpperCase(Locale.ROOT);
        if (upperName.endsWith("JOB_SEQ")) {
            return JOB_INSTANCE_TABLE;
        }
        if (upperName.endsWith("JOB_EXECUTION_SEQ")) {
            return JOB_EXECUTION_TABLE;
        }
        if (upperName.endsWith("STEP_EXECUTION_SEQ")) {
            return STEP_EXECUTION_TABLE;
        }

        throw new IllegalArgumentException("Unsupported 批量任务 incrementer name: " + incrementerName);
    }

    private static final class SegmentContextMaxValueIncrementer implements DataFieldMaxValueIncrementer {

        private final String tableName;

        private SegmentContextMaxValueIncrementer(String tableName) {
            this.tableName = tableName;
        }

        @Override
        public int nextIntValue() {
            long nextValue = nextLongValue();
            if (nextValue > Integer.MAX_VALUE) {
                throw new DataAccessResourceFailureException("Batch id overflow for table " + tableName);
            }
            return (int) nextValue;
        }

        @Override
        public long nextLongValue() {
            Long nextValue = SegmentContextHelper.nextId(tableName);
            if (nextValue == null || nextValue <= 0L) {
                throw new DataAccessResourceFailureException("Failed to allocate id for table " + tableName);
            }
            return nextValue;
        }

        @Override
        public String nextStringValue() {
            return String.valueOf(nextLongValue());
        }
    }
}
