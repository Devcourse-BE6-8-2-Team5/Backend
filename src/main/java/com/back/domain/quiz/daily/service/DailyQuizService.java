package com.back.domain.quiz.daily.service;

import com.back.domain.quiz.daily.entity.DailyQuiz;
import com.back.domain.quiz.daily.repository.DailyQuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyQuizService {
    private final DailyQuizRepository dailyQuizRepository;

    @Transactional(readOnly = true)
    public List<DailyQuiz> getDailyQuizzes(Long todayNewsId) {
        return dailyQuizRepository.findByTodayNewsId(todayNewsId);
    }
}
