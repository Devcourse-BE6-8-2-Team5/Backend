package com.back.domain.news.common.service;

import com.back.domain.news.common.dto.AnalyzedNewsDto;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.global.ai.AiService;
import com.back.global.ai.processor.NewsAnalysisProcessor;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisNewsService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;

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

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();
    }

    @Async("analysisExecutor")
    public CompletableFuture<List<AnalyzedNewsDto>> processBatchAsync(List<RealNewsDto> batch) {
        try {
            // Rate limiting - 토큰 얻을 때까지 계속 시도
            waitForRateLimit();

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

    // 토큰 얻을때까지 대기
    private void waitForRateLimit() throws InterruptedException {
        int attempts = 0;
        while (!bucket.tryConsume(1)) {
            attempts++;
            log.debug("Rate limit 대기 중... 시도 횟수: {}", attempts);
            Thread.sleep(2000); // 2초 대기

            // 너무 오래 기다리면 경고 (하지만 포기하지 않음)
            if (attempts % 10 == 0) {
                log.warn("Rate limit 대기가 길어지고 있습니다. 대기 횟수: {}", attempts);
            }
        }

        if (attempts > 0) {
            log.debug("Rate limit 토큰 획득 - 대기 횟수: {}", attempts);
        }
    }
}
