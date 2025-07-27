package com.back.domain.news.common.service;

import com.back.domain.news.common.entity.KeywordGenerationReqDto;
import com.back.domain.news.common.entity.KeywordGenerationResDto;
import com.back.global.ai.AiService;
import com.back.global.ai.processor.KeywordGeneratorProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public KeywordGenerationResDto generateTodaysKeywords() {
        LocalDate today = LocalDate.now();

        List<String> excludeKeywords = getExcludeKeywords();

        KeywordGenerationReqDto req = new KeywordGenerationReqDto(today, excludeKeywords);

        log.info("키워드 생성 요청 - 날짜 :  {} , 제외 키워드 : {}", today, excludeKeywords);

        KeywordGeneratorProcessor processor = new KeywordGeneratorProcessor(req, objectMapper);
        KeywordGenerationResDto result = aiService.process(processor);

        log.info("키워드 생성 결과 - {}", result);
        log.debug("생된 키워드 - {}", result.getAllKeywords());

//        KeywordHistoryService.saveKeywordHistory(result.getAllKeywords(), today);

        return result;
    }

    private List<String> getExcludeKeywords() {
        List<String> excludeKeywords = new ArrayList<>();

//        // 1. 최근 3일간 2회 이상 사용된 키워드 (과도한 반복 방지)
//        excludeKeywords.addAll(getOverusedKeywords(3, 2));
//
//        // 2. 어제 사용된 키워드 중 일반적인 것들 (긴급 뉴스 제외)
//        excludeKeywords.addAll(getYesterdayCommonKeywords());
//
//        // 3. 특정 키워드의 쿨타임 관리 (예: "대통령" 키워드는 3일간 사용 후 1일 휴식)
//        excludeKeywords.addAll(getCooldownKeywords());

        return excludeKeywords.stream().distinct().toList();

    }

}
