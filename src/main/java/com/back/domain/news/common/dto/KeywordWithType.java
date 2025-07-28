package com.back.domain.news.common.dto;

import com.back.domain.news.common.enums.KeywordType;

public record KeywordWithType(
        String keyword,
        KeywordType type
) {
}
