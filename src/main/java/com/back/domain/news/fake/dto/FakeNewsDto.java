package com.back.domain.news.fake.dto;

public record FakeNewsDto (
        Long realNewsId,
        String title,
        String content
) {
    public static FakeNewsDto of(Long realNewsId, String title, String content) {
        return new FakeNewsDto(realNewsId, title, content);
    }
}
