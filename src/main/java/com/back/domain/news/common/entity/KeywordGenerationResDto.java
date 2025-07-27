package com.back.domain.news.common.entity;

import java.time.LocalDate;
import java.util.List;

public record KeywordGenerationResDto(
        List<String> society,
        List<String> economy,
        List<String> politics,
        List<String> culture,
        List<String> it
) {
    public List<String> getAllKeywords() {
        return List.of(society, economy, politics, culture, it)
                .stream()
                .flatMap(List::stream)
                .toList();
    }
}
