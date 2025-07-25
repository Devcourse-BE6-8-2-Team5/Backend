package com.back.domain.quiz.detail.dto;

import com.back.domain.quiz.detail.entity.Option;

public record DetailQuizResDto(
        String question,
        String option1,
        String option2,
        String option3,
        Option correctOption
) {
}

