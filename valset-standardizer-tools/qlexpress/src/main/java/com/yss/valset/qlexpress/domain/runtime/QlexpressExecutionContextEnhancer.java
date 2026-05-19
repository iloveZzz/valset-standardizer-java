package com.yss.valset.qlexpress.domain.runtime;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * QLExpress 执行上下文增强器，只注入显式允许暴露给脚本的对象。
 */
@Component
public class QlexpressExecutionContextEnhancer {

    private final ObjectProvider<QlexpressContextContributor> contributorProvider;
    private final List<QlexpressContextContributor> directContributors;

    @Autowired
    public QlexpressExecutionContextEnhancer(ObjectProvider<QlexpressContextContributor> contributorProvider) {
        this.contributorProvider = contributorProvider;
        this.directContributors = null;
    }

    public QlexpressExecutionContextEnhancer(List<QlexpressContextContributor> directContributors) {
        this.contributorProvider = null;
        this.directContributors = directContributors == null ? Collections.emptyList() : directContributors;
    }

    public Map<String, Object> enhance(String runnerScope, Map<String, Object> context) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (context != null && !context.isEmpty()) {
            result.putAll(context);
        }
        if (directContributors != null) {
            for (QlexpressContextContributor contributor : directContributors) {
                if (contributor != null) {
                    contributor.contribute(runnerScope, result);
                }
            }
            return result;
        }
        if (contributorProvider != null) {
            contributorProvider.orderedStream().forEach(contributor -> contributor.contribute(runnerScope, result));
        }
        return result;
    }

    public static QlexpressExecutionContextEnhancer empty() {
        return new QlexpressExecutionContextEnhancer(Collections.emptyList());
    }
}
