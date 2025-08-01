package com.back.global.rateLimiter;

import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimiterConfig {
    @Bean
    public Bucket bucket() {
        return Bucket.builder()
                .addLimit(limit ->
                        limit.capacity(30).refillIntervally(1, Duration.ofSeconds(2))) // 2초마다 1개 토큰 보충 (1분에 30개)
                .build();
    }
}
