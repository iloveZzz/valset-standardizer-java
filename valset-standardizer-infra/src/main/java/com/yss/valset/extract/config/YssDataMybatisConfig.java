package com.yss.valset.extract.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = true)
@MapperScan(basePackages = {"com.yss.valset.extract.repository.mapper"})
public class YssDataMybatisConfig {
}
