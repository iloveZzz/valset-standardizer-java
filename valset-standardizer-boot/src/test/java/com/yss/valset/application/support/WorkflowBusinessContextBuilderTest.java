package com.yss.valset.application.support;

import com.yss.valset.application.command.MatchTaskCommand;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.extract.application.command.ExtractDataTaskCommand;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowBusinessContextBuilderTest {

    private final WorkflowBusinessContextBuilder builder = new WorkflowBusinessContextBuilder();

    @Test
    void shouldBuildParseContextWithBusinessFields() {
        ParseTaskCommand command = new ParseTaskCommand();
        command.setDataSourceType("CSV");
        command.setWorkbookPath("/tmp/parse.csv");
        command.setFileId(1001L);

        Map<String, Object> context = builder.build(command);

        assertThat(context)
                .containsEntry("dataSourceType", "CSV")
                .containsEntry("workbookPath", "/tmp/parse.csv")
                .containsEntry("fileId", 1001L)
                .doesNotContainKey("createdBy")
                .doesNotContainKey("forceRebuild");
    }

    @Test
    void shouldBuildMatchContextWithTopK() {
        MatchTaskCommand command = new MatchTaskCommand();
        command.setDataSourceType("EXCEL");
        command.setWorkbookPath("/tmp/match.xlsx");
        command.setFileId(2002L);
        command.setTopK(7);

        Map<String, Object> context = builder.build(command);

        assertThat(context)
                .containsEntry("dataSourceType", "EXCEL")
                .containsEntry("workbookPath", "/tmp/match.xlsx")
                .containsEntry("fileId", 2002L)
                .containsEntry("topK", 7)
                .doesNotContainKey("createdBy");
    }

    @Test
    void shouldBuildExtractContextWithFingerprint() {
        ExtractDataTaskCommand command = new ExtractDataTaskCommand();
        command.setDataSourceType("EXCEL");
        command.setWorkbookPath("/tmp/extract.xlsx");
        command.setFileId(3003L);
        command.setFileFingerprint("fingerprint-001");
        command.setFilesysTaskId("filesys-task-1");
        command.setFilesysFileId("filesys-file-1");
        command.setFilesysObjectKey("object-key-1");
        command.setFilesysInstantUpload(Boolean.TRUE);

        Map<String, Object> context = builder.build(command);

        assertThat(context)
                .containsEntry("dataSourceType", "EXCEL")
                .containsEntry("workbookPath", "/tmp/extract.xlsx")
                .containsEntry("fileId", 3003L)
                .containsEntry("fileFingerprint", "fingerprint-001")
                .containsEntry("filesysTaskId", "filesys-task-1")
                .containsEntry("filesysFileId", "filesys-file-1")
                .containsEntry("filesysObjectKey", "object-key-1")
                .containsEntry("filesysInstantUpload", Boolean.TRUE)
                .doesNotContainKey("createdBy");
    }
}
