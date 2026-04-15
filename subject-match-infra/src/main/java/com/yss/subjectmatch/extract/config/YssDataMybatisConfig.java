package com.yss.subjectmatch.extract.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = true)
@MapperScan(basePackages = {"com.yss.subjectmatch.extract.repository.mapper"})
public class YssDataMybatisConfig {
}
