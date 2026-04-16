package com.yss.subjectmatch.extract.standardization.mapping;

/**
 * 表头映射查找接口。
 */
public interface HeaderMappingLookup {

    HeaderMappingCandidate findExact(String text);

    HeaderMappingCandidate findAliasContains(String text);
}
