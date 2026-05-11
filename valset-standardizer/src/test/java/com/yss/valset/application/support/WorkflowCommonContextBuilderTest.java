package com.yss.valset.application.support;

import com.yss.valset.application.command.EvaluateMappingTaskCommand;
import com.yss.valset.application.command.MatchTaskCommand;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.extract.application.command.ExtractDataTaskCommand;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowCommonContextBuilderTest {

    private final WorkflowCommonContextBuilder builder = new WorkflowCommonContextBuilder();

    @Test
    void shouldBuildParseCommonContext() {
        ParseTaskCommand command = new ParseTaskCommand();
        command.setCreatedBy("zhudaoming");
        command.setForceRebuild(Boolean.TRUE);

        Map<String, Object> context = builder.build(command);

        assertThat(context)
                .containsEntry("createdBy", "zhudaoming")
                .containsEntry("forceRebuild", Boolean.TRUE);
    }

    @Test
    void shouldBuildMatchCommonContext() {
        MatchTaskCommand command = new MatchTaskCommand();
        command.setCreatedBy("zhudaoming");

        Map<String, Object> context = builder.build(command);

        assertThat(context)
                .containsEntry("createdBy", "zhudaoming")
                .containsEntry("forceRebuild", Boolean.FALSE);
    }

    @Test
    void shouldBuildExtractCommonContext() {
        ExtractDataTaskCommand command = new ExtractDataTaskCommand();
        command.setCreatedBy("zhudaoming");
        command.setForceRebuild(Boolean.TRUE);

        Map<String, Object> context = builder.build(command);

        assertThat(context)
                .containsEntry("createdBy", "zhudaoming")
                .containsEntry("forceRebuild", Boolean.TRUE);
    }

    @Test
    void shouldBuildEvaluateCommonContext() {
        EvaluateMappingTaskCommand command = new EvaluateMappingTaskCommand();
        command.setCreatedBy("zhudaoming");

        Map<String, Object> context = builder.build(command);

        assertThat(context)
                .containsEntry("createdBy", "zhudaoming")
                .containsEntry("forceRebuild", Boolean.FALSE);
    }
}
