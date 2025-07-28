package com.back.domain.quiz.detail.service;

import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.repository.RealNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetailQuizScheduledService {
    private final DetailQuizAsyncService detailQuizAsyncService;
    private final RealNewsRepository realNewsRepository;

    // 매일 오전 5시에 오늘 DB에 저장된 뉴스로 퀴즈 생성
    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    public void generateQuizzesForTodayNews() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay(); // 오늘 00:00
        LocalDateTime end = today.plusDays(1).atStartOfDay(); // 내일 00:00

        // 오늘 날짜의 뉴스 조회(시간 설정은 추후 변경 가능)
        List<RealNews> todayNews = realNewsRepository.findTodayNews(start, end);

        if (todayNews.isEmpty()) {
            log.info("오늘 날짜의 뉴스 없음. 퀴즈 생성을 건너뜁니다.");
            return;
        }

        log.info("퀴즈 생성 시작. 오늘 날짜의 뉴스 수: " + todayNews.size());

        // 모든 뉴스에 대해 비동기 처리 (Rate Limiter가 속도 조절)
        List<CompletableFuture<Void>> futures = todayNews.stream()
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
