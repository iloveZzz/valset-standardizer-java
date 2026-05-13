package com.yss.valset.knowledge;

import com.yss.valset.domain.gateway.StandardSubjectGateway;
import com.yss.valset.domain.knowledge.StandardSubjectLoader;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.StandardSubject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于数据库落地表的标准科目加载器。
 */
@Slf4j
@Component
public class DbStandardSubjectLoader implements StandardSubjectLoader {

    private final StandardSubjectGateway standardSubjectGateway;

    public DbStandardSubjectLoader(StandardSubjectGateway standardSubjectGateway) {
        this.standardSubjectGateway = standardSubjectGateway;
    }

    /**
     * 从标准科目落地表加载标准科目。
     */
    @Override
    public List<StandardSubject> load(DataSourceConfig config) {
        log.info("开始从标准科目落地表加载数据，sourceUri={}", config == null ? null : config.getSourceUri());
        List<StandardSubject> subjects = standardSubjectGateway.findAll();
        log.info("标准科目落地表加载完成，count={}", subjects == null ? 0 : subjects.size());
        return subjects;
    }
}
