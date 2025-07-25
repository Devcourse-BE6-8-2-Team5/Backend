package com.back.domain.news.util;

import lombok.Getter;

@Getter
public enum NewsType {
    REAL("진짜"),
    FAKE("가짜");

    private final String description;

    NewsType(String description) {
        this.description = description;
    }

}
