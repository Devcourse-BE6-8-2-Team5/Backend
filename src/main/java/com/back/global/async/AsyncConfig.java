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
        executor.initialize();
        return executor;
    }
}
