package com.back.domain.news.fake.dto;

public record FakeNewsDto (
        Long realNewsId,
        String title,
        String content
) {
}
