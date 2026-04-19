package com.yss.valset.extract.rule;

import com.yss.valset.extract.repository.entity.ParseRuleProfilePO;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 解析模板运行时解析器。
 */
public interface ParseRuleTemplateResolver {

    /**
     * 解析已发布模板。
     */
    ParseRuleProfilePO resolvePublishedProfile(String fileScene, String fileTypeName);

    /**
     * 解析表头规则表达式。
     */
    String resolveHeaderExpr(String fileScene, String fileTypeName);

    /**
     * 解析行分类规则表达式。
     */
    String resolveRowClassifyExpr(String fileScene, String fileTypeName);

    /**
     * 解析字段映射规则表达式。
     */
    String resolveFieldMapExpr(String fileScene, String fileTypeName);

    /**
     * 解析值转换规则表达式。
     */
    String resolveTransformExpr(String fileScene, String fileTypeName);

    /**
     * 解析表头必选字段。
     */
    List<String> resolveRequiredHeaders(String fileScene, String fileTypeName);

    /**
     * 解析科目代码正则表达式。
     */
    Pattern resolveSubjectCodePattern(String fileScene, String fileTypeName);
}
