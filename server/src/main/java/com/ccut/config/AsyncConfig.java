package com.ccut.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * 异步线程池配置
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 聊天异步线程池
     * 用于处理聊天相关的异步任务，如缓存刷新
     */
    @Bean("chatExecutor")
    public Executor chatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(10);
        // 最大线程数
        executor.setMaxPoolSize(20);
        // 队列容量
        executor.setQueueCapacity(100);
        // 线程名称前缀
        executor.setThreadNamePrefix("chat-async-");
        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 缓存异步线程池
     * 用于处理缓存相关的异步任务
     */
    @Bean("cacheExecutor")
    public Executor cacheExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("cache-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 学习进度上报异步线程池
     * 专门处理学习时长计算和进度上报，避免阻塞主线程
     * 核心配置说明：
     * - 核心线程数20：应对日常学习时长上报请求
     * - 最大线程数50：应对高峰期（如上课时间集中学习）
     * - 队列容量500：缓冲突发请求
     * - 拒绝策略：记录日志并降级处理（不阻塞用户）
     */
    @Bean("progressExecutor")
    public Executor progressExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：根据实际并发调整，建议20-50
        executor.setCorePoolSize(20);
        // 最大线程数：高峰期扩展能力
        executor.setMaxPoolSize(50);
        // 队列容量：缓冲积压请求
        executor.setQueueCapacity(500);
        // 线程名称前缀：便于日志追踪
        executor.setThreadNamePrefix("progress-async-");
        // 拒绝策略：队列满时记录日志并降级，不影响主业务
        executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                // 记录告警日志
                org.slf4j.LoggerFactory.getLogger(AsyncConfig.class)
                    .warn("进度上报线程池队列已满，丢弃进度更新请求。活动线程: {}, 队列大小: {}",
                        e.getActiveCount(), e.getQueue().size());
                // 不抛异常，避免影响主线程
            }
        });
        // 优雅关闭：等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
}
