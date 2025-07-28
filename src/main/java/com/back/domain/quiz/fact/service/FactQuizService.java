package com.back.domain.quiz.fact.service;

import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.quiz.fact.entity.FactQuiz;
import com.back.domain.quiz.fact.repository.FactQuizRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FactQuizService {
    private final FactQuizRepository factQuizRepository;

    @Transactional(readOnly = true)
    public List<FactQuiz> findAll() {
        return factQuizRepository.findAllWithNews();
    }

    @Transactional(readOnly = true)
    public List<FactQuiz> findByCategory(NewsCategory category) {
        return factQuizRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public FactQuiz findById(Long id) {
        return factQuizRepository.findByIdWithNews(id)
                .orElseThrow(() -> new ServiceException(404, "팩트 퀴즈를 찾을 수 없습니다. ID: " + id));
    }
}
