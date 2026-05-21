package com.yss.valset.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

@Configuration(proxyBeanMethods = true)
@MapperScan(basePackages = {"com.yss.cloud.**.repository", "com.yss.cloud.**.mapper", "com.yss.valset.**.mapper"})
public class YssDataMybatisConfig {

    @Bean
    public static BeanPostProcessor mybatisJdbcTypeForNullPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof SqlSessionFactory) {
                    ((SqlSessionFactory) bean).getConfiguration().setJdbcTypeForNull(JdbcType.NULL);
                }
                return bean;
            }
        };
    }
}
