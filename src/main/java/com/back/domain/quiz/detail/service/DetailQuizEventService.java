package com.back.domain.quiz.detail.service;

import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.repository.RealNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetailQuizEventService {
    private final DetailQuizAsyncService detailQuizAsyncService;
    private final RealNewsRepository realNewsRepository;

    public void generateDetailQuizzes(List<Long> realNewsIds) {
        List<RealNews> realNewsList = realNewsRepository.findAllById(realNewsIds);

        if (realNewsList.isEmpty()) {
            log.info("there is no real news to generate quizzes for. Skipping quiz generation. News IDs: " + realNewsIds);
            return;
        }

        log.info("상세 퀴즈 생성 시작. 뉴스 개수: " + realNewsList.size());

        // 모든 뉴스에 대해 비동기 처리 (Rate Limiter가 속도 조절)
        List<CompletableFuture<Void>> futures = realNewsList.stream()
                .map(news -> detailQuizAsyncService.generateAsync(news.getId()))
                .toList();

        // 모든 작업 완료 대기 (옵션)
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        allOf.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("일부 퀴즈 생성 작업이 실패했습니다.", throwable);
            } else {
                log.info("모든 퀴즈 생성 작업이 완료되었습니다.");
            }
        });
    }
}
