package com.back.global.ai.processor;

import com.back.domain.news.common.dto.KeywordGenerationReqDto;
import com.back.domain.news.common.dto.KeywordGenerationResDto;
import com.back.domain.news.fake.dto.FakeNewsDto;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 진짜 뉴스를 기반으로 가짜 뉴스를 생성하는 AI 요청 Processor 입니다.
 */
@Slf4j
public class FakeNewsGeneratorProcessor implements AiRequestProcessor<FakeNewsDto> {
    private final RealNewsDto req;
    private final ObjectMapper objectMapper;

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 10000;


    public FakeNewsGeneratorProcessor(RealNewsDto req, ObjectMapper objectMapper) {
        this.req = req;
        this.objectMapper = objectMapper;
    }

    // 진짜 뉴스를 바탕으로 가짜 뉴스 생성용 프롬프트 생성
    @Override
    public String buildPrompt() {
        return String.format("""
               Task: 제공된 진짜 뉴스의 제목을 바탕으로 가짜 뉴스를 생성하라.
               
               목적:
               - 진짜 뉴스와 유사한 형식과 문체로 작성
               - 사용자가 진짜와 가짜를 구분하기 어려울 정도로 정교하게 작성
               - 실제 존재할 법한 내용으로 구성하되, 사실과는 다른 내용 포함
               
               ⚠️ 중요한 제약사항:
               - 문체, 문장 구조, 단락 구성을 원본과 유사하게 작성
               - 날짜, 인물명, 기관명 등은 적절히 변경하되 현실성 있게 구성
               
               [원본 뉴스 정보]
               제목: %s
               내용: %s
               카테고리: %s
               
               [가짜 뉴스 생성 요구사항]
               1. 제목: 진짜 뉴스와 동일하게 작성
               2. 내용: 원본의 문체와 구조를 따라 작성하되 완전히 다른 내용
               3. 기사 내 언론사명과 기자 명이 있다면 형식 유지 (예: [데일리안 = 기자명] 형식)
               4. 인용문, 수치, 기관명 등은 그럴듯하게 변경
               
               [JSON 출력 규칙 - 매우 중요]
               - 마크다운 코드 블록 사용 금지
               - 아래 JSON 형식으로만 응답
               - 문자열 내 따옴표가 있으면 역슬래시로 이스케이프

               {
                 "realNewsId": %s,
                 "title": "그대로 사용",
                 "content": "가짜 뉴스 본문"
               }
               """,
                cleanText(req.title()),
                cleanText(req.content()),
                req.newsCategory(),
                req.id());
    }

    // AI 응답을 파싱하여 FakeNewsDto로 변환
    @Override
    public FakeNewsDto parseResponse(ChatResponse response) {
        String text = response.getResult().getOutput().getText();
        if (text == null || text.trim().isEmpty()) {
            throw new ServiceException(500, "AI 응답이 비어있습니다");
        }

        try {
            String cleanedJson = cleanResponse(text);
            FakeNewsDto result = objectMapper.readValue(cleanedJson, FakeNewsDto.class);
            validateResult(result);
            return result;

        } catch (Exception e) {
            log.warn("JSON 파싱 실패, 폴백 실행. 오류: {}", e.getMessage());
            return createFallback();
        }
    }
    /**
     * AI 응답 정리 - 마크다운 코드 블록만 제거
     */
    private String cleanResponse(String text) {
        return text.trim()
                .replaceAll("(?s)```json\\s*(.*?)\\s*```", "$1")
                .replaceAll("```", "")
                .trim();
    }

    /**
     * 프롬프트용 텍스트 정리
     */
    private String cleanText(String text) {
        if (text == null) return "";
        return text.replace("\"", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 결과 검증
     */
    private void validateResult(FakeNewsDto result) {
        if (result.realNewsId() == null ||
                result.title() == null || result.title().trim().isEmpty() ||
                result.content() == null || result.content().trim().isEmpty()) {
            throw new IllegalArgumentException("필수 필드 누락");
        }
    }

    /**
     * 파싱 실패시 폴백
     */
    private FakeNewsDto createFallback() {
        return FakeNewsDto.of(
                req.id(),
                req.title() + " (AI 생성 실패)",
                "이 뉴스는 AI 생성에 실패하여 기본값으로 대체되었습니다."
        );
    }
}