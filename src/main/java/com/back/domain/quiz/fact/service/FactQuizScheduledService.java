package com.back.domain.quiz.fact.service;

import com.back.domain.news.fake.entity.FakeNews;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.domain.quiz.fact.repository.FactQuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactQuizScheduledService {
    private final FactQuizRepository factQuizRepository;
    private final RealNewsRepository realNewsRepository;
    private final FactQuizService factQuizService;

    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul") // 시간 추후 변경(가짜 퀴즈 생성 후)
    public void generateFactQuizzes() {
        try {
            LocalDateTime start = LocalDate.now().atStartOfDay(); // 오늘 00:00
            LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay(); // 내일 00:00

            // 오늘 날짜의 뉴스 조회(시간 설정은 추후 변경 가능)
            List<RealNews> todayNews = realNewsRepository.findTodayNews(start, end);

            if (todayNews.isEmpty()) {
                log.info("오늘 날짜의 뉴스 없음. 퀴즈 생성을 건너뜁니다.");
                return;
            }

            // 이미 퀴즈가 생성된 뉴스 ID Set 조회
            Set<Long> alreadyQuizGeneratedIds = factQuizRepository.findRealNewsIdsWithFactQuiz(start, end);

            int successCount = 0;
            int failureCount = 0;

            for(RealNews real : todayNews) {
                try {
                    // 이미 퀴즈가 생성된 뉴스는 건너뜁니다.
                    if (alreadyQuizGeneratedIds.contains(real.getId())) {
                        log.debug("이미 퀴즈가 생성된 뉴스: {}. {}", real.getId(), real.getTitle());
                        continue;
                    }

                    FakeNews fake = real.getFakeNews();

                    // 가짜 뉴스가 없는 경우 건너뜁니다.
                    if (fake == null) {
                        log.warn("가짜 뉴스가 없는 뉴스: {}. {}", real.getId(), real.getTitle());
                        continue;
                    }

                    // 개별 트랜잭션으로 퀴즈 생성
                    factQuizService.create(real, fake);
                    successCount++;

                } catch (Exception e) {
                    failureCount++;
                    log.error("팩트 퀴즈 생성 실패. 뉴스 ID: {}, 에러: {}", real.getId(), e.getMessage(), e);
                }
            }

            log.info("팩트 퀴즈 생성 배치 완료. 성공: {}개, 건너뜀: {}개, 실패: {}개", successCount, todayNews.size()-successCount-failureCount, failureCount);

        } catch (Exception e) {
            log.error("팩트 퀴즈 생성 배치 중 예외 발생: {}", e.getMessage(), e);
        }
    }
}
