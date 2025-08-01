package com.back.global.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "quizExecutor")
    public Executor quizExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 동시에 실행할 스레드 수
        executor.setMaxPoolSize(10);  // 최대 스레드 수
        executor.setQueueCapacity(100); // 대기 큐 크기
        executor.setThreadNamePrefix("QuizGen-");
        executor.setWaitForTasksToCompleteOnShutdown(true); // 종료 시 모든 작업이 완료될 때까지 대기
        executor.setAwaitTerminationSeconds(30); // 종료 대기 시간
        executor.initialize();
        return executor;
    }

    @Bean(name = "analysisExecutor")
    public Executor analysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 동시에 실행할 스레드 수
        executor.setMaxPoolSize(10);  // 최대 스레드 수
        executor.setQueueCapacity(100); // 대기 큐 크기
        executor.setThreadNamePrefix("QuizGen-");
        executor.setWaitForTasksToCompleteOnShutdown(true); // 종료 시 모든 작업이 완료될 때까지 대기
        executor.setAwaitTerminationSeconds(100); // 종료 대기 시간
        executor.initialize();
        return executor;
    }

}
