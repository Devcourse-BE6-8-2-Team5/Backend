package com.back.domain.quiz.detail.dto;

public record DetailQuizReqDto(
        String title,
        String content
) {
    public DetailQuizReqDto(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
