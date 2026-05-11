package com.yss.valset.extract.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = true)
@MapperScan(basePackages = {"com.yss.cloud.**.repository", "com.yss.cloud.**.mapper", "com.yss.valset.**.mapper"})
public class YssDataMybatisConfig {
}
