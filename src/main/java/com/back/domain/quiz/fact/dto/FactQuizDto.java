package com.back.domain.quiz.fact.dto;

import com.back.domain.quiz.fact.entity.FactQuiz;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FactQuizDto(
        @NotNull Long id,
        @NotBlank String question,
        @NotBlank String realNewsTitle
) {
    public FactQuizDto(FactQuiz quiz) {
        this(
                quiz.getId(),
                quiz.getQuestion(),
                quiz.getRealNews().getTitle()
        );
    }
}
