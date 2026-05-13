package com.yss.valset.knowledge;

import com.yss.valset.domain.gateway.MappingSampleGateway;
import com.yss.valset.domain.knowledge.MappingSampleLoader;
import com.yss.valset.domain.model.MappingSample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于数据库落地表的映射评估样本加载器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DbMappingSampleLoader implements MappingSampleLoader {

    private final MappingSampleGateway mappingSampleGateway;

    /**
     * 从映射样例落地表加载数据。
     */
    @Override
    public List<MappingSample> load() {
        log.info("开始从映射样例落地表加载数据");
        List<MappingSample> samples = mappingSampleGateway.findAll();
        log.info("映射样例落地表加载完成，count={}", samples == null ? 0 : samples.size());
        return samples;
    }
}
