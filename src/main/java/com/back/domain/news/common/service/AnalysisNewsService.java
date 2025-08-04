package com.back.domain.news.common.service;

import com.back.domain.news.common.dto.AnalyzedNewsDto;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.global.ai.AiService;
import com.back.global.ai.processor.NewsAnalysisProcessor;
import com.back.global.rateLimiter.RateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisNewsService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;

    @Qualifier("bucket")
    private final Bucket bucket;

    @Value("${news.filter.batch.size:2}") // 배치 크기 설정, 3으로 하면 본문 길면 깨짐
    private int batchSize;


    public List<AnalyzedNewsDto> filterAndScoreNews(List<RealNewsDto> allRealNewsBeforeFilter) {
        if (allRealNewsBeforeFilter == null || allRealNewsBeforeFilter.isEmpty()) {
            log.warn("필터링할 뉴스가 없습니다.");
            return List.of();
        }

        log.info("뉴스 필터링 시작 - 총 {}개", allRealNewsBeforeFilter.size());

        List<List<RealNewsDto>> batches= new ArrayList<>();

        // batch_size로 나누어서 처리
        for (int i = 0; i < allRealNewsBeforeFilter.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, allRealNewsBeforeFilter.size());
            batches.add(allRealNewsBeforeFilter.subList(i, endIndex));
        }

        List<AnalyzedNewsDto> allResults = Collections.synchronizedList(new ArrayList<>());

        // 비동기 작업들 시작
        List<CompletableFuture<Void>> futures = batches.stream()
                .map(batch -> processBatchAsync(batch)
                        .thenAccept(result -> {
                            // 완료되는 대로 즉시 결과에 추가
                            allResults.addAll(result);
                            log.info("배치 완료 - 현재까지 {}개 처리됨", allResults.size());
                        })
                        .exceptionally(throwable -> {
                            log.error("배치 처리 실패", throwable);
                            return null;
                        }))
                .toList();

        try {
            // 모든 작업 완료 대기
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("뉴스 분석이 중단되었습니다", cause);
            }
            log.error("뉴스 분석 중 일부 오류 발생했지만 계속 진행", cause);
        }

        log.info("뉴스 필터링 완료 - 최종 결과: {}개", allResults.size());
        return new ArrayList<>(allResults);
    }

    @Async("newsExecutor")
    public CompletableFuture<List<AnalyzedNewsDto>> processBatchAsync(List<RealNewsDto> batch) {

        log.info("스레드: {}, 배치 시작 시간: {}",
                Thread.currentThread().getName(), System.currentTimeMillis());
        try {
            // Rate limiting - 토큰 얻을 때까지 계속 시도
            rateLimiter.waitForRateLimit();
            log.debug("배치 처리 시작 - 기사 수: {}", batch.size());

            NewsAnalysisProcessor processor = new NewsAnalysisProcessor(batch, objectMapper);
            List<AnalyzedNewsDto> result = aiService.process(processor);

            log.debug("배치 처리 완료 - 결과 수: {}", result.size());

            log.info("스레드: {}, 배치 완료 시간: {}",
                    Thread.currentThread().getName(), System.currentTimeMillis());

            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("배치 {}개 처리 중 오류 발생", batch.size(), e);
            return CompletableFuture.completedFuture(List.of()); // AI 처리 실패시만 빈 리스트
        }
    }

}
