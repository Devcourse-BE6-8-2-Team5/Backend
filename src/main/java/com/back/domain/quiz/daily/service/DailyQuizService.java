package com.back.domain.quiz.daily.service;

import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.repository.TodayNewsRepository;
import com.back.domain.news.today.entity.TodayNews;
import com.back.domain.quiz.daily.entity.DailyQuiz;
import com.back.domain.quiz.daily.repository.DailyQuizRepository;
import com.back.domain.quiz.detail.entity.DetailQuiz;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyQuizService {
    private final DailyQuizRepository dailyQuizRepository;
    private final TodayNewsRepository todayNewsRepository;

    @Transactional(readOnly = true)
    public List<DailyQuiz> getDailyQuizzes(Long todayNewsId) {
        return dailyQuizRepository.findByTodayNewsId(todayNewsId);
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void createDailyQuiz() {
        TodayNews todayNews = todayNewsRepository.findFirstByOrderBySelectedDateDesc()
                .orElseThrow(() -> new ServiceException(400, "오늘의 뉴스가 없습니다."));

        boolean alreadyCreated = dailyQuizRepository.existsByTodayNews(todayNews);

        if (alreadyCreated) {
            log.info("이미 오늘의 퀴즈가 생성되었습니다. 작업을 건너뜁니다.");
            return;
        }

        RealNews realNews = todayNews.getRealNews();

        List<DetailQuiz> quizzes = realNews.getDetailQuizzes();

        if (quizzes == null || quizzes.isEmpty()) {
            throw new ServiceException(400, "연결된 상세 퀴즈가 없습니다.");
        }

        for(DetailQuiz quiz : quizzes) {
            DailyQuiz dailyQuiz = new DailyQuiz(todayNews, quiz);
            dailyQuizRepository.save(dailyQuiz);
        }

        log.info("오늘의 퀴즈 생성 완료");
    }
}
