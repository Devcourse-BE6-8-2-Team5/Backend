package com.back.domain.quiz.daily.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DailyQuizScheduledService {
    private final DailyQuizService dailyQuizService;

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    public void scheduleCreateDailyQuiz() {
        dailyQuizService.createDailyQuiz();
    }
}
