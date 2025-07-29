package com.back.domain.news.common.service;

import com.back.domain.news.common.dto.AnalyzedNewsDto;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.global.ai.AiService;
import com.back.global.ai.processor.NewsAnalysisProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsAnalysisService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public List<AnalyzedNewsDto> filterAndScoreNews(List<RealNewsDto> allRealNewsBeforeFilter) {
        if (allRealNewsBeforeFilter == null || allRealNewsBeforeFilter.isEmpty()) {
            log.warn("필터링할 뉴스가 없습니다.");
            return List.of();
        }

        log.info("뉴스 필터링 시작 - 총 {}개", allRealNewsBeforeFilter.size());

        List<AnalyzedNewsDto> filteredResults = new ArrayList<>();

        // 3개씩 나누어서 처리
        for (int i = 0; i < allRealNewsBeforeFilter.size(); i += 3) {
            int endIndex = Math.min(i + 3, allRealNewsBeforeFilter.size());
            List<RealNewsDto> batch = allRealNewsBeforeFilter.subList(i, endIndex);

            try {
                log.debug("배치 처리 중: {}-{}", i + 1, endIndex);

                NewsAnalysisProcessor processor = new NewsAnalysisProcessor(batch, objectMapper);
                List<AnalyzedNewsDto> analyzedNewsDtos = aiService.process(processor);

                filteredResults.addAll(analyzedNewsDtos);

            } catch (Exception e) {
                log.error("배치 {}개 처리 중 오류 발생, 건너뜀: {}", batch.size(), e.getMessage());
                // 오류 발생 시 해당 배치는 건너뛰고 다음 배치 계속 처리
                continue;
            }
        }

        log.info("뉴스 필터링 완료 - 총 {}개 중 {}개 처리됨", allRealNewsBeforeFilter.size(), filteredResults.size());
        return filteredResults;
    }
}
