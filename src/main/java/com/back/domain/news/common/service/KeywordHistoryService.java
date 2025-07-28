package com.back.domain.news.common.service;


import com.back.domain.news.common.dto.KeywordGenerationResDto;
import com.back.domain.news.common.dto.KeywordWithType;
import com.back.domain.news.common.entity.KeywordHistory;
import com.back.domain.news.common.repository.KeywordHistoryRepository;
import com.back.domain.news.util.NewsCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KeywordHistoryService {

    private final KeywordHistoryRepository keywordHistoryRepository;

    @Transactional
    public void saveKeywords(KeywordGenerationResDto keywords, LocalDate usedDate){

        // Save each keyword category to the repository
        saveKeywordsByCategory(keywords.society(), NewsCategory.SOCIETY, usedDate);
        saveKeywordsByCategory(keywords.economy(), NewsCategory.ECONOMY, usedDate);
        saveKeywordsByCategory(keywords.politics(), NewsCategory.POLITICS, usedDate);
        saveKeywordsByCategory(keywords.culture(), NewsCategory.CULTURE, usedDate);
        saveKeywordsByCategory(keywords.it(), NewsCategory.IT, usedDate);

    }


    private void saveKeywordsByCategory(
            List<KeywordWithType> keywords,
            NewsCategory category,
            LocalDate usedDate) {

        List<KeywordHistory> keywordHistories = keywords.stream()
                .map(keyword -> KeywordHistory.builder()
                        .keywordWithType(keyword)
                        .category(category)
                        .usedDate(usedDate)
                        .build())
                .toList();

        keywordHistoryRepository.saveAll(keywordHistories);
    }

        // 1. 최근 5일간 3회 이상 사용된 키워드 (과도한 반복 방지)
    @Transactional(readOnly = true)
    public List<String> getOverusedKeywords(int days, int minUsage) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        return keywordHistoryRepository.findOverusedKeywords(startDate, minUsage);
    }

        // 2. 어제 사용된 키워드 중 일반적인 것들 (긴급 뉴스 제외)
    @Transactional(readOnly = true)
    public List<String> getYesterdayKeywords() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return keywordHistoryRepository.findKeywordsByUsedDate(yesterday);
    }

}
