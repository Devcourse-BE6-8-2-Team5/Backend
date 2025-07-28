package com.back.domain.news.common.enums;

import lombok.Getter;

@Getter
public enum NewsCategory {
    SOCIETY("사회"),
    ECONOMY("경제"),
    POLITICS("정치"),
    CULTURE("문화"),
    IT("IT");

    private String description;

    NewsCategory(String description) {
        this.description = description;
    }
}
