package com.yss.valset.common.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskFailureClassifierTest {

    @Test
    void resolveReadableMessageSkipsBatchStepWrapper() {
        IllegalArgumentException realCause = new IllegalArgumentException("未识别到必选表头：科目代码");
        IllegalStateException wrapper = new IllegalStateException("批量任务 文件解析阶段失败，taskId=2056220357769052162", realCause);

        assertEquals("未识别到必选表头：科目代码", TaskFailureClassifier.resolveReadableMessage(wrapper));
    }

    @Test
    void resolveReadableMessageUsesDeepestNonWrapperMessage() {
        RuntimeException realCause = new RuntimeException("ORA-00933: SQL 命令未正确结束");
        IllegalStateException stepWrapper = new IllegalStateException("批量任务 估值贴源数据落地阶段失败，taskId=1", realCause);
        IllegalStateException jobWrapper = new IllegalStateException("批量任务 作业执行失败：批量任务 估值贴源数据落地阶段失败，taskId=1", stepWrapper);

        assertEquals("ORA-00933: SQL 命令未正确结束", TaskFailureClassifier.resolveReadableMessage(jobWrapper));
    }
}
