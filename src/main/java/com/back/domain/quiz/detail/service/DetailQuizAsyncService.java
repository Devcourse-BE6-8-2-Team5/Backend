package com.back.domain.quiz.detail.service;

import com.back.domain.quiz.detail.dto.DetailQuizDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetailQuizAsyncService {
    private final DetailQuizService detailQuizService;

    @Async("quizExecutor")
    public void generateAsync(long newsId) {
        int maxRetries = 3; // 최대 재시도 횟수
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                // Ai 호출해 퀴즈 생성(트랜잭션 없음)
                List<DetailQuizDto> quizzes = detailQuizService.generateQuizzes(newsId);

                // 생성된 퀴즈를 DB에 저장(트랜잭션)
                detailQuizService.saveQuizzes(newsId, quizzes);

                return; // 성공 시 종료
            } catch (Exception e) { // 예외 발생 시 재시도(최대 재시도 횟수까지)
                attempt++;
                if (attempt == maxRetries) { // 마지막 시도에서도 실패하면 예외 발생
                    log.error("퀴즈 생성에 실패했습니다. 뉴스 ID: " + newsId + ", 오류: " + e.getMessage(), e);
                }
                try {
                    Thread.sleep(2000); // 2초 대기 후 재시도
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // 인터럽트 복구
                    log.warn("퀴즈 생성 재시도 대기 중 인터럽트가 발생했습니다. 뉴스 ID: " + newsId, ie);
                }
            }
        }
    }
}
