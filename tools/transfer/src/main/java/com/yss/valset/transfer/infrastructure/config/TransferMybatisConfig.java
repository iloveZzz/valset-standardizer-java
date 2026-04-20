package com.yss.valset.transfer.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 文件收发分拣模块的 MyBatis 配置。
 */
@Configuration(proxyBeanMethods = true)
@MapperScan(basePackages = {"com.yss.valset.transfer.infrastructure.repository.mapper"})
public class TransferMybatisConfig {
}
