package com.back.domain.quiz.daily.controller;

import com.back.domain.quiz.daily.dto.DailyQuizDto;
import com.back.domain.quiz.daily.entity.DailyQuiz;
import com.back.domain.quiz.daily.service.DailyQuizService;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quiz/daily")
@RequiredArgsConstructor
public class DailyQuizController {
    private final DailyQuizService dailyQuizService;

    @GetMapping("/{todayNewsId}")
    public RsData<List<DailyQuizDto>> getDailyQuizzes(@PathVariable Long todayNewsId) {
        List<DailyQuiz> dailyQuizzes = dailyQuizService.getDailyQuizzes(todayNewsId);
        return RsData.of(
                200,
                "오늘의 퀴즈 조회 성공",
                dailyQuizzes.stream()
                        .map(DailyQuizDto::new)
                        .toList()
        );
    }
}
