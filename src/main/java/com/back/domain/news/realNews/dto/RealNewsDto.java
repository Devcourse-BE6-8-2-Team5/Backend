package com.back.domain.news.realNews.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Json;

import java.time.LocalDateTime;

public record RealNewsDto(
        String title,
        String content,
        String description,
        String link,
        String imgUrl,
        LocalDateTime originCreatedDate,
        String mediaName,
        String journalist,
        String originalNewsUrl
) {
    public static RealNewsDto of(
            String title,
            String content,
            String description,
            String link,
            String imgUrl,
            LocalDateTime originCreatedDate,
            String mediaName,
            String journalist,
            String originalNewsUrl
    ) {
        return new RealNewsDto(
                title, content, description, link, imgUrl, originCreatedDate, mediaName, journalist, originalNewsUrl
        );
    }

    public static RealNewsDto from(JsonNode item){
        return new RealNewsDto(
                item.get("title").asText(),
                item.get("content").asText(),
                item.get("description").asText(),
                item.get("link").asText(),
                item.get("imgUrl").asText(),
                LocalDateTime.parse(item.get("originCreatedDate").asText()),
                item.get("mediaName").asText(),
                item.get("journalist").asText(),
                item.get("originalNewsUrl").asText()
        );
    }
}



