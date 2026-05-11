package com.yss.valset.config;

import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer;
import com.github.kagkarlsson.scheduler.jdbc.JdbcCustomization;
import com.github.kagkarlsson.scheduler.SchedulerName;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import com.github.kagkarlsson.scheduler.serializer.Serializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * db-scheduler 基础配置。
 */
@Configuration
public class DbSchedulerConfiguration {

    /**
     * 使用 Jackson 作为任务数据序列化器。
     */
    @Bean
    public DbSchedulerCustomizer dbSchedulerCustomizer() {
        return new DbSchedulerCustomizer() {
            @Override
            public Optional<SchedulerName> schedulerName() {
                return Optional.empty();
            }

            @Override
            public Optional<Serializer> serializer() {
                return Optional.of(new JacksonSerializer());
            }

            @Override
            public Optional<ExecutorService> executorService() {
                return Optional.empty();
            }

            @Override
            public Optional<ExecutorService> dueExecutor() {
                return Optional.empty();
            }

            @Override
            public Optional<ScheduledExecutorService> housekeeperExecutor() {
                return Optional.empty();
            }

            @Override
            public Optional<JdbcCustomization> jdbcCustomization() {
                return Optional.empty();
            }

            @Override
            public Optional<javax.sql.DataSource> dataSource() {
                return Optional.empty();
            }
        };
    }
}
