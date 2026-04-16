package com.yss.subjectmatch.extract.standardization.mapping;

import com.yss.subjectmatch.domain.model.MappingDecision;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultHeaderMappingEngineTest {

    @Test
    void should_apply_exact_segment_alias_and_fallback_in_order() {
        HeaderMappingEngine engine = new DefaultHeaderMappingEngine();
        HeaderMappingLookup lookup = new HeaderMappingLookup() {
            @Override
            public HeaderMappingCandidate findExact(String text) {
                return switch (text) {
                    case "科目代码" -> new HeaderMappingCandidate(1L, 11L, "subject_cd");
                    case "数量" -> new HeaderMappingCandidate(2L, 12L, "n_hldamt");
                    default -> null;
                };
            }

            @Override
            public HeaderMappingCandidate findAliasContains(String text) {
                if (text != null && text.contains("估值增值")) {
                    return new HeaderMappingCandidate(3L, 13L, "n_hldvva");
                }
                return null;
            }
        };

        List<HeaderMappingInput> inputs = List.of(
                new HeaderMappingInput(0, "科目代码", List.of("基础信息", "科目代码")),
                new HeaderMappingInput(1, "持仓信息|数量", List.of("持仓信息", "数量")),
                new HeaderMappingInput(2, "证券估值增值(本币)", List.of("证券估值增值(本币)")),
                new HeaderMappingInput(3, "未知列", List.of("未知列"))
        );
        Map<Integer, MappingDecision> decisions = engine.map(inputs, lookup);

        assertThat(decisions).hasSize(4);
        assertThat(decisions.get(0).getStrategy()).isEqualTo("exact_header");
        assertThat(decisions.get(0).getStandardCode()).isEqualTo("subject_cd");
        assertThat(decisions.get(1).getStrategy()).isEqualTo("header_segment");
        assertThat(decisions.get(1).getStandardCode()).isEqualTo("n_hldamt");
        assertThat(decisions.get(2).getStrategy()).isEqualTo("alias_contains");
        assertThat(decisions.get(2).getStandardCode()).isEqualTo("n_hldvva");
        assertThat(decisions.get(3).getStrategy()).isEqualTo("fallback");
        assertThat(decisions.get(3).getMatched()).isFalse();
    }
}
