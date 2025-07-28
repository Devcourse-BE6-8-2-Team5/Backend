package com.back.domain.quiz.fact.service;

import com.back.domain.news.fake.entity.FakeNews;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.domain.quiz.fact.entity.CorrectNewsType;
import com.back.domain.quiz.fact.entity.FactQuiz;
import com.back.domain.quiz.fact.repository.FactQuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactQuizScheduledService {
    private final FactQuizRepository factQuizRepository;
    private final RealNewsRepository realNewsRepository;

    @Transactional
    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul") // 시간 추후 변경(가짜 퀴즈 생성 후)
    public void generateFactQuizzes() {
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

        for(RealNews real : todayNews) {
            // 이미 퀴즈가 생성된 뉴스는 건너뜁니다.
            if (alreadyQuizGeneratedIds.contains(real.getId())) {
                log.info("이미 퀴즈가 생성된 뉴스: {}. {}", real.getId(), real.getTitle());
                continue;
            }

            FakeNews fake = real.getFakeNews();

            // 가짜 뉴스가 없는 경우 건너뜁니다.
            if (fake == null) {
                log.info("가짜 뉴스가 없는 뉴스: {}. {}", real.getId(), real.getTitle());
                continue;
            }

            // 퀴즈 질문과 정답은 랜덤으로 생성
            CorrectNewsType answerType = ThreadLocalRandom.current().nextBoolean()
                    ? CorrectNewsType.REAL
                    : CorrectNewsType.FAKE;

            String question = answerType == CorrectNewsType.REAL
                    ? "다음 중 진짜 뉴스는?"
                    : "다음 중 가짜 뉴스는?";

            // 팩트 퀴즈 생성 및 저장
            FactQuiz quiz = new FactQuiz(question, real, fake, answerType);
            //real.getFactQuizzes().add(quiz);
            //fake.getFactQuizzes().add(quiz);
            factQuizRepository.save(quiz);

            log.info("팩트 퀴즈 생성 완료. 퀴즈 ID: {}, 뉴스 ID: {}", quiz.getId(), real.getId());
        }
    }
}
