package com.ccut.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * 异步线程池配置
 * 为不同类型的异步任务提供专用线程池
 */
@Slf4j
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
        
        log.info("聊天异步线程池初始化完成：corePoolSize=10, maxPoolSize=20, queueCapacity=100");
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
        
        log.info("缓存异步线程池初始化完成：corePoolSize=5, maxPoolSize=10, queueCapacity=50");
        return executor;
    }

    /**
     * 学习进度上报异步线程池
     * 专门处理学习时长计算和进度上报，避免阻塞主线程
     * 核心配置说明：
     * - 核心线程数 20：应对日常学习时长上报请求
     * - 最大线程数 50：应对高峰期（如上课时间集中学习）
     * - 队列容量 500：缓冲突发请求
     * - 拒绝策略：记录日志并降级处理（不阻塞用户）
     */
    @Bean("progressExecutor")
    public Executor progressExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：根据实际并发调整，建议 20-50
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
                log.warn("进度上报线程池队列已满，丢弃进度更新请求。活动线程：{}, 队列大小：{}, 核心线程：{}, 最大线程：{}",
                    e.getActiveCount(), e.getQueue().size(), e.getCorePoolSize(), e.getMaximumPoolSize());
                // 不抛异常，避免影响主线程
            }
        });
        // 优雅关闭：等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        
        log.info("学习进度异步线程池初始化完成：corePoolSize=20, maxPoolSize=50, queueCapacity=500");
        return executor;
    }

    /**
     * 文档分析专用线程池
     * 用于异步读取和解析上传的各类文档
     */
    @Bean("documentAnalysisExecutor")
    public Executor documentAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("doc-analysis-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("文档分析线程池初始化完成：corePoolSize=4, maxPoolSize=8, queueCapacity=50");
        return executor;
    }
}
