package com.yss.valset.extract.config;

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import java.util.Properties;

@Configuration(proxyBeanMethods = true)
@MapperScan(basePackages = {"com.yss.cloud.**.repository", "com.yss.cloud.**.mapper", "com.yss.valset.**.mapper"})
public class YssDataMybatisConfig {
}
