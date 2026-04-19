package com.yss.valset.knowledge.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 知识模块的 MyBatis 扫描配置。
 * <p>
 * 用于注册数据库标准科目加载器中定义的 Mapper 接口。
 * </p>
 */
@Configuration(proxyBeanMethods = false)
@MapperScan(basePackages = "com.yss.valset.knowledge")
public class KnowledgeMybatisConfig {
}
