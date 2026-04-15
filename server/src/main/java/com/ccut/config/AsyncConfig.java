package com.ccut.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;


@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 聊天异步线程池
     */
    @Bean("chatExecutor")
    public Executor chatExecutor() {
        return createExecutor("chat-async-", 10, 20, 100,
                new ThreadPoolExecutor.CallerRunsPolicy(), 60);
    }

    /**
     * 缓存异步线程池
     */
    @Bean("cacheExecutor")
    public Executor cacheExecutor() {
        return createExecutor("cache-async-", 5, 10, 50,
                new ThreadPoolExecutor.CallerRunsPolicy(), 60);
    }

    /**
     * 学习进度上报异步线程池
     * 核心线程数 20，最大线程数 50，队列容量 500
     * 拒绝策略：队列满时记录日志并丢弃，不影响主业务
     */
    @Bean("progressExecutor")
    public Executor progressExecutor() {
        return createExecutor("progress-async-", 20, 50, 500,
                (r, e) -> log.warn("进度上报线程池队列已满，丢弃请求。活动线程：{}, 队列大小：{}",
                        e.getActiveCount(), e.getQueue().size()), 120);
    }

    /**
     * 文档分析专用线程池
     */
    @Bean("documentAnalysisExecutor")
    public Executor documentAnalysisExecutor() {
        return createExecutor("doc-analysis-", 4, 8, 50,
                new ThreadPoolExecutor.CallerRunsPolicy(), 60);
    }

    private Executor createExecutor(String namePrefix, int corePoolSize, int maxPoolSize,
                                    int queueCapacity, RejectedExecutionHandler rejectedHandler,
                                    int awaitTerminationSeconds) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(namePrefix);
        executor.setRejectedExecutionHandler(rejectedHandler);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();

        log.info("线程池初始化完成：{} corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                namePrefix, corePoolSize, maxPoolSize, queueCapacity);
        return executor;
    }
}
