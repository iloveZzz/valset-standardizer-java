package com.yss.subjectmatch;

import com.yss.cloud.EnableDistributedId;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 主题匹配服务的应用程序引导程序。
 */
@EnableAsync
@EnableDiscoveryClient
@EnableAspectJAutoProxy
@EnableDistributedId
@SpringBootApplication(scanBasePackages = {"com.yss"})
public class SubjectMatchBootApplication {
    /**
     * 启动Spring应用程序。
     */
    public static void main(String[] args) {
        SpringApplication.run(SubjectMatchBootApplication.class, args);
    }
}
