package org.planmoni.safehavenservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async Configuration for Background Processing.
 * 
 * Production Considerations:
 * - Dedicated thread pool for webhook processing (isolated from other async tasks)
 * - Graceful shutdown with await termination
 * - Bounded queue to prevent memory exhaustion
 * - Rejected execution handler for overflow scenarios
 * - Thread naming for monitoring and debugging
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    @Value("${spring.task.execution.pool.core-size:5}")
    private int corePoolSize;

    @Value("${spring.task.execution.pool.max-size:10}")
    private int maxPoolSize;

    @Value("${spring.task.execution.pool.queue-capacity:100}")
    private int queueCapacity;

    @Value("${spring.task.execution.shutdown.await-termination:true}")
    private boolean awaitTermination;

    @Value("${spring.task.execution.shutdown.await-termination-period:60s}")
    private String awaitTerminationPeriod;

    /**
     * Thread pool executor for webhook processing.
     * Isolated from other async tasks for better resource management.
     */
    @Bean(name = "webhookProcessingExecutor")
    public Executor webhookProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("safehaven-webhook-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(parseSeconds(awaitTerminationPeriod));
        
        // Rejected execution handler: Caller runs policy (synchronous fallback)
        // In production, consider custom handler with metrics/alerts
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        
        log.info("Initialized webhook processing executor: core={}, max={}, queue={}", 
                 corePoolSize, maxPoolSize, queueCapacity);
        
        return executor;
    }

    /**
     * General purpose async executor for other background tasks.
     * Separate from webhook processing for isolation.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(Math.max(2, corePoolSize / 2));
        executor.setMaxPoolSize(Math.max(4, maxPoolSize / 2));
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("safehaven-async-");
        executor.setWaitForTasksToCompleteOnShutdown(awaitTermination);
        executor.setAwaitTerminationSeconds(parseSeconds(awaitTerminationPeriod));
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        
        log.info("Initialized general async executor: core={}, max={}, queue={}", 
                 executor.getCorePoolSize(), executor.getMaxPoolSize(), queueCapacity);
        
        return executor;
    }

    /**
     * Parses duration string (e.g., "60s") to seconds.
     */
    private int parseSeconds(String period) {
        if (period == null || period.isEmpty()) {
            return 60;
        }
        
        try {
            if (period.endsWith("s")) {
                return Integer.parseInt(period.substring(0, period.length() - 1));
            } else {
                return Integer.parseInt(period);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid await termination period format: {}, using default 60s", period);
            return 60;
        }
    }
}
