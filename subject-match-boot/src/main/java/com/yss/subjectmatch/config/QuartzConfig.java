package com.yss.subjectmatch.config;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.AdaptableJobFactory;

/**
 * 石英集成配置。
 */
@Configuration
public class QuartzConfig {

    /**
     * 保证Quartz作业可以由Spring自动装配。
     */
    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(ApplicationContext applicationContext) {
        return factory -> factory.setJobFactory(new SpringContextJobFactory(applicationContext.getAutowireCapableBeanFactory()));
    }

    private static class SpringContextJobFactory extends AdaptableJobFactory {

        private final AutowireCapableBeanFactory beanFactory;

        private SpringContextJobFactory(AutowireCapableBeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        /**
         * 通过Spring创建Quartz作业实例。
         */
        @Override
        protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
            return beanFactory.createBean(bundle.getJobDetail().getJobClass());
        }
    }
}
