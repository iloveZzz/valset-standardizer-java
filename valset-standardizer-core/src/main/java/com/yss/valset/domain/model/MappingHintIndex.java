package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;

/**
 * 历史映射提示的内存索引。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingHintIndex {
    private List<MappingHint> hints;
    private Map<String, List<MappingHint>> hintsByName;
    private Map<String, List<MappingHint>> hintsByCode;

    /**
     * 通过规范化的外部名称查找提示。
     */
    public List<MappingHint> findNameHints(String normalizedKey) {
        if (normalizedKey == null || normalizedKey.isBlank() || hintsByName == null) {
            return List.of();
        }
        return hintsByName.getOrDefault(normalizedKey, List.of());
    }

    /**
     * 通过外部代码查找提示。
     */
    public List<MappingHint> findCodeHints(String subjectCode) {
        if (subjectCode == null || subjectCode.isBlank() || hintsByCode == null) {
            return List.of();
        }
        return hintsByCode.getOrDefault(subjectCode.trim(), List.of());
    }

    /**
     * 由历史映射提示列表构建索引。
     */
    public static MappingHintIndex fromHints(List<MappingHint> hints) {
        List<MappingHint> safeHints = hints == null ? List.of() : new ArrayList<>(hints);
        Map<String, List<MappingHint>> hintsByName = safeHints.stream()
                .filter(hint -> hint != null && hint.getNormalizedKey() != null && !hint.getNormalizedKey().isBlank())
                .collect(Collectors.groupingBy(hint -> hint.getNormalizedKey().trim(), LinkedHashMap::new,
                        Collectors.toCollection(ArrayList::new)));
        Map<String, List<MappingHint>> hintsByCode = safeHints.stream()
                .filter(hint -> hint != null && hint.getStandardCode() != null && !hint.getStandardCode().isBlank())
                .collect(Collectors.groupingBy(hint -> hint.getStandardCode().trim(), LinkedHashMap::new,
                        Collectors.toCollection(ArrayList::new)));
        return MappingHintIndex.builder()
                .hints(safeHints.stream().filter(item -> item != null).toList())
                .hintsByName(hintsByName)
                .hintsByCode(hintsByCode)
                .build();
    }
}
