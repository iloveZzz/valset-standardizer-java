package com.yss.valset.config;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Component;

/**
 * 绑定 MyBatis-Plus 全局 SqlSessionFactory，确保批量插入工具方法可用。
 */
@Component
@RequiredArgsConstructor
public class MybatisPlusGlobalConfigBinder {

    private final SqlSessionFactory sqlSessionFactory;

    @PostConstruct
    public void bind() {
        GlobalConfig globalConfig = GlobalConfigUtils.getGlobalConfig(sqlSessionFactory.getConfiguration());
        globalConfig.setSqlSessionFactory(sqlSessionFactory);
        GlobalConfigUtils.setGlobalConfig(sqlSessionFactory.getConfiguration(), globalConfig);
    }
}
