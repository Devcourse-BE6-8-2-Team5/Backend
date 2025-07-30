package com.back.domain.quiz.detail.dto;

import com.back.domain.quiz.detail.entity.DetailQuiz;
import com.back.domain.quiz.detail.entity.Option;

public record DetailQuizDto(
        String question,
        String option1,
        String option2,
        String option3,
        Option correctOption
) {
    public DetailQuizDto(DetailQuiz detailQuiz) {
        this(
                detailQuiz.getQuestion(),
                detailQuiz.getOption1(),
                detailQuiz.getOption2(),
                detailQuiz.getOption3(),
                detailQuiz.getCorrectOption()
        );
    }
}

