package com.gym.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    // 配置异步线程池
    @Bean("mailAsyncExecutor")
    public Executor mailAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("MailAsyncExecutor-");
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        // 返回默认的异步线程池，如果没有特别指定可用同一个
        return mailAsyncExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        // 捕获异步方法中未处理的异常
        return (throwable, method, params) -> {
            // 记录日志或者通知运维团队
            log.error("Async error in method: " + method.getName(), throwable);
        };
    }
}

