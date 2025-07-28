package com.back.domain.quiz.fact.dto;

import com.back.domain.quiz.QuizType;
import com.back.domain.quiz.fact.entity.CorrectNewsType;
import com.back.domain.quiz.fact.entity.FactQuiz;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FactQuizDtoWithNewsContent(
        @NotNull Long id,
        @NotBlank String question,
        @NotBlank String realNewsTitle,
        @NotBlank String realNewsContent,
        @NotBlank String fakeNewsContent,
        @NotNull CorrectNewsType correctNewsType,
        @NotNull QuizType quizType
        ) {
    public FactQuizDtoWithNewsContent(FactQuiz quiz) {
        this(
                quiz.getId(),
                quiz.getQuestion(),
                quiz.getRealNews().getTitle(),
                quiz.getRealNews().getContent(),
                quiz.getFakeNews().getContent(),
                quiz.getCorrectNewsType(),
                quiz.getQuizType()
        );
    }
}
