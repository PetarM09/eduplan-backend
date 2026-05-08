package rs.skola.platforma.common.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Value("${app.async.core-pool-size:4}")
    private int corePool;

    @Value("${app.async.max-pool-size:16}")
    private int maxPool;

    @Value("${app.async.queue-capacity:100}")
    private int queue;

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePool);
        executor.setMaxPoolSize(maxPool);
        executor.setQueueCapacity(queue);
        executor.setThreadNamePrefix("skolska-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error(
                "Neuhvacena greska u asinhronoj metodi {}.{}",
                method.getDeclaringClass().getSimpleName(), method.getName(), ex);
    }
}
