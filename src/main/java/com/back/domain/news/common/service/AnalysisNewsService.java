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
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

        List<CompletableFuture<List<AnalyzedNewsDto>>> futures = batches.stream()
                .map(this::processBatchAsync)
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

            List<AnalyzedNewsDto> allResults = futures.stream()
                    .map(CompletableFuture::join) // 이미 완료된 상태라 즉시 반환
                    .flatMap(List::stream)
                    .toList();

            log.info("뉴스 필터링 완료 - 결과: {}개", allResults.size());
            return allResults;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("뉴스 분석이 중단되었습니다", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("뉴스 분석 중 오류 발생", e.getCause());
        }
    }

    @Async("newsExecutor")
    public CompletableFuture<List<AnalyzedNewsDto>> processBatchAsync(List<RealNewsDto> batch) {
        try {
            // Rate limiting - 토큰 얻을 때까지 계속 시도
            rateLimiter.waitForRateLimit();
            log.debug("배치 처리 시작 - 기사 수: {}", batch.size());

            NewsAnalysisProcessor processor = new NewsAnalysisProcessor(batch, objectMapper);
            List<AnalyzedNewsDto> result = aiService.process(processor);

            log.debug("배치 처리 완료 - 결과 수: {}", result.size());
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("배치 {}개 처리 중 오류 발생", batch.size(), e);
            return CompletableFuture.completedFuture(List.of()); // AI 처리 실패시만 빈 리스트
        }
    }

}
