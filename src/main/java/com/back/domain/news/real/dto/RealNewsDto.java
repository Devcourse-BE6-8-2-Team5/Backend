package com.back.domain.news.real.dto;

import com.back.domain.news.common.enums.NewsCategory;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record RealNewsDto(
        Long id,
        String title,
        String content,
        String description,
        String link,
        String imgUrl,
        LocalDateTime originCreatedDate,
        String mediaName,
        String journalist,
        String originalNewsUrl,
        NewsCategory newsCategory
) {
    public static RealNewsDto of(
            Long id,
            String title,
            String content,
            String description,
            String link,
            String imgUrl,
            LocalDateTime originCreatedDate,
            String mediaName,
            String journalist,
            String originalNewsUrl,
            NewsCategory newsCategory
    ) {
        return new RealNewsDto(
                id, title, content, description, link, imgUrl, originCreatedDate, mediaName, journalist, originalNewsUrl, newsCategory
        );
    }

    public static RealNewsDto from(JsonNode item){
        return new RealNewsDto(
                item.get("id").asLong(),
                item.get("title").asText(),
                item.get("content").asText(),
                item.get("description").asText(),
                item.get("link").asText(),
                item.get("imgUrl").asText(),
                LocalDateTime.parse(item.get("originCreatedDate").asText()),
                item.get("mediaName").asText(),
                item.get("journalist").asText(),
                item.get("originalNewsUrl").asText(),
                parseNewsCategory(item.get("newsCategory").asText())
        );
    }
    private static NewsCategory parseNewsCategory(String categoryStr) {
        try {
            return NewsCategory.valueOf(categoryStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NewsCategory.NOT_FILTERED;
        }
    }

}



