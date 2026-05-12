package com.yss.valset.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.yss.valset.common.support.DatabaseDialectSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 分页方言配置。
 */
@Configuration(proxyBeanMethods = false)
public class MybatisPlusDialectConfiguration {

    /**
     * 依据当前数据源自动选择分页方言。
     */
    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(DatabaseDialectSupport databaseDialectSupport) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(resolveDbType(databaseDialectSupport)));
        return interceptor;
    }

    private DbType resolveDbType(DatabaseDialectSupport databaseDialectSupport) {
        if (databaseDialectSupport.isOracle()) {
            return DbType.ORACLE;
        }
        if (databaseDialectSupport.isPostgreSql()) {
            return DbType.POSTGRE_SQL;
        }
        return DbType.MYSQL;
    }
}
