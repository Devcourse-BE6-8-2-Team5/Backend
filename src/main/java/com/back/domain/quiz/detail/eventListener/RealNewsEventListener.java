package com.back.domain.quiz.detail.eventListener;

import com.back.domain.news.real.event.RealNewsCreatedEvent;
import com.back.domain.quiz.detail.service.DetailQuizEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RealNewsEventListener {
    private final DetailQuizEventService detailQuizEventService;

    @EventListener
    public void handleRealNewsCreated(RealNewsCreatedEvent event) {
        log.info("🔥 TodayNewsCreatedEvent 수신. 이벤트 발생: {}", event);
        List<Long> realNewsIds = event.getRealNewsIds();

        try {
            detailQuizEventService.generateDetailQuizzes(realNewsIds);
        } catch (Exception e) {
            log.error("팩트 퀴즈 생성 중 오류 발생: {}", e.getMessage(), e);
        }

    }
}
