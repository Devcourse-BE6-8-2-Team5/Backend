package com.back.domain.quiz.fact.service;

import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.quiz.fact.entity.FactQuiz;
import com.back.domain.quiz.fact.repository.FactQuizRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FactQuizService {
    private final FactQuizRepository factQuizRepository;

    public List<FactQuiz> findAll() {
        return factQuizRepository.findAll();
    }

    public List<FactQuiz> findByCategory(NewsCategory category) {
        return factQuizRepository.findByCategory(category);
    }

    public FactQuiz findById(Long id) {
        return factQuizRepository.findById(id)
                .orElseThrow(() -> new ServiceException(404, "팩트 퀴즈를 찾을 수 없습니다. ID: " + id));
    }
}
