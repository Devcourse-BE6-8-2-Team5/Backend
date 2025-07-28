package com.back.domain.news.common.service;

import com.back.domain.news.common.dto.KeywordGenerationReqDto;
import com.back.domain.news.common.dto.KeywordGenerationResDto;
import com.back.global.ai.AiService;
import com.back.global.ai.processor.KeywordGeneratorProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeywordGenerationService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final KeywordHistoryService keywordHistoryService;

    @Value("${keyword.overuse.days}")
    private int overuseDays;

    @Value("${keyword.overuse.threshold}")
    private int overuseThreshold;

    /**
     * 오늘 날짜에 맞춰 키워드를 생성합니다.
     * - 최근 5일간 3회 이상 사용된 키워드 제외 (yml에서 overuseDays, overuseThreshold 설정값을 통해 조절)
     * - 어제 사용된 일반적인 키워드 제외
     *
     * @return 생성된 키워드 결과
     */
    public KeywordGenerationResDto generateTodaysKeywords() {
        LocalDate today = LocalDate.now();

        List<String> excludeKeywords = getExcludeKeywords();

        KeywordGenerationReqDto req = new KeywordGenerationReqDto(today, excludeKeywords);

        log.info("키워드 생성 요청 - 날짜 :  {} , 제외 키워드 : {}", today, excludeKeywords);

        KeywordGeneratorProcessor processor = new KeywordGeneratorProcessor(req, objectMapper);
        KeywordGenerationResDto result = aiService.process(processor);

        log.info("키워드 생성 결과 - {}", result);

//        keywordHistoryService.saveKeywords(result, today);

        return result;
    }

    private List<String> getExcludeKeywords() {
        List<String> excludeKeywords = new ArrayList<>();

        // 1. 최근 5일간 3회 이상 사용된 키워드 (과도한 반복 방지)
        excludeKeywords.addAll(keywordHistoryService.getOverusedKeywords(overuseDays, overuseThreshold));
        // 2. 어제 사용된 키워드 중 일반적인 것들 (긴급 뉴스 제외)
        excludeKeywords.addAll(keywordHistoryService.getYesterdayKeywords());

        log.debug("제외 키워드 목록: {}", excludeKeywords);
        return excludeKeywords.stream().distinct().toList();
    }

}
