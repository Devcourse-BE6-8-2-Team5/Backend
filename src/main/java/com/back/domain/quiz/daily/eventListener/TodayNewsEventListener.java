package com.back.domain.quiz.daily.eventListener;

import com.back.domain.news.today.event.TodayNewsCreatedEvent;
import com.back.domain.quiz.daily.service.DailyQuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TodayNewsEventListener {
    private final DailyQuizService dailyQuizService;

    @EventListener
    public void handleFakeNewsCreated(TodayNewsCreatedEvent event) {
        Long todayNewsId = event.getTodayNewsId();

        try {
            // 오늘의 뉴스 ID를 사용하여 오늘의 퀴즈 생성
            dailyQuizService.createDailyQuiz(todayNewsId);
        } catch (Exception e) {
            log.error("오늘의 뉴스 ID {}에 대한 오늘의 퀴즈 생성 중 오류 발생: {}", todayNewsId, e.getMessage(), e);
        }
    }
}
