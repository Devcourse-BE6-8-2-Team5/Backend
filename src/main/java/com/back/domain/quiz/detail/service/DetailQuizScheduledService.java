package com.back.domain.quiz.detail.service;

import com.back.domain.news.realNews.entity.RealNews;
import com.back.domain.news.realNews.repository.RealNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetailQuizScheduledService {
    private final DetailQuizAsyncService detailQuizAsyncService;
    private final RealNewsRepository realNewsRepository;

    // 매일 오전 5시에 오늘 DB에 저장된 뉴스로 퀴즈 생성
    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul") // 매일 오전 5시에 실행(시간은 나중에 변경)
    public void generateQuizzesForTodayNews() {
        // 뉴스 엔티티 변형 필요해 상의 후 진행
        // List<RealNews> todayNews = realNewsRepository.findTodayNews(); // 오늘 날짜의 뉴스 조회

        // 임시로 전체 뉴스 조회 (나중에 오늘 날짜의 뉴스로 변경)
        List<RealNews> todayNews = realNewsRepository.findAll();

        if (todayNews.isEmpty()) {
            log.info("오늘 날짜의 뉴스가 없습니다. 퀴즈 생성을 건너뜁니다.");
            return;
        }

        int batchSize = 20; // 한 번에 처리할 뉴스 수(실패 재시도 고려해 여유있게 설정)
        for(int i=0; i<todayNews.size(); i+=batchSize) {
            int end = Math.min(i + batchSize, todayNews.size());
            List<RealNews> batch = todayNews.subList(i, end);
            for(RealNews news : batch) {
                detailQuizAsyncService.generateAsync(news.getId());
            }
            try{
                // 배치 처리 후 1분 대기 (API 호출 제한을 피하기 위해)
                Thread.sleep(60_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 인터럽트 복구
                log.error("퀴즈 생성 대기 중 인터럽트가 발생했습니다: "+ e.getMessage());
            }
        }
    }
}
