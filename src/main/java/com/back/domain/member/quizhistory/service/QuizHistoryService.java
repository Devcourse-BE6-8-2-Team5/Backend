package com.back.domain.member.quizhistory.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.quizhistory.dto.QuizHistoryDto;
import com.back.domain.member.quizhistory.entity.QuizHistory;
import com.back.domain.member.quizhistory.repository.QuizHistoryRepository;
import com.back.domain.quiz.QuizType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizHistoryService {

    private final QuizHistoryRepository quizHistoryRepository;

    @Transactional(readOnly = true)
    public List<QuizHistoryDto> getQuizHistoriesByMember(Member actor) {
        // Member의 quizHistories 리스트를 바로 가져오기 (Lazy라면 강제로 초기화 필요)
        List<QuizHistory> quizHistories = actor.getQuizHistories();

        // 필요하면 정렬 - createdDate 내림차순으로 정렬
        quizHistories.sort((a, b) -> b.getCreatedDate().compareTo(a.getCreatedDate()));

        return quizHistories.stream()
                .map(QuizHistoryDto::new)
                .toList();
    }

    @Transactional
    public void save(Member actor, Long id,QuizType quizType, String answer, boolean isCorrect, int gainExp) {

        QuizHistory quizHistory = QuizHistory.builder()
                .member(actor)
                .quizId(id)
                .quizType(quizType)
                .answer(answer)
                .isCorrect(isCorrect)
                .gainExp(gainExp)
                .build();

        // 퀴즈 히스토리 저장
        quizHistoryRepository.save(quizHistory);
    }
}
