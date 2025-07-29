package com.back.global.ai.processor;

import com.back.domain.news.common.dto.KeywordGenerationReqDto;
import com.back.domain.news.common.dto.KeywordGenerationResDto;
import com.back.domain.news.fake.dto.FakeNewsDto;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * 진짜 뉴스를 기반으로 가짜 뉴스를 생성하는 AI 요청 Processor 입니다.
 */
public class FakeNewsGeneratorProcessor implements AiRequestProcessor<FakeNewsDto> {
    private final RealNewsDto req;
    private final ObjectMapper objectMapper;

    public FakeNewsGeneratorProcessor(RealNewsDto req, ObjectMapper objectMapper) {
        this.req = req;
        this.objectMapper = objectMapper;
    }

    // 진짜 뉴스를 바탕으로 가짜 뉴스 생성용 프롬프트 생성
    @Override
    public String buildPrompt() {
        return String.format("""
               Task: 제공된 진짜 뉴스를 바탕으로 가짜 뉴스를 생성하라.
               
               목적:
               - 진짜 뉴스와 유사한 형식과 문체로 작성
               - 사용자가 진짜와 가짜를 구분하기 어려울 정도로 정교하게 작성
               - 실제 존재할 법한 내용으로 구성하되, 사실과는 다른 내용 포함
               
               ⚠️ 중요한 제약사항:
               - 문체, 문장 구조, 단락 구성을 원본과 유사하게 작성
               - 날짜, 인물명, 기관명 등은 적절히 변경하되 현실성 있게 구성
               - 원본 뉴스와 같은 카테고리 내에서 다른 주제로 작성
               
               [원본 뉴스 정보]
               제목: %s
               내용: %s
               카테고리: %s
               
               [가짜 뉴스 생성 요구사항]
               1. 제목: 원본과 유사한 톤앤매너로 작성
               2. 내용: 원본의 문체와 구조를 따라 작성하되 완전히 다른 내용
               3. 기사 내 언론사명과 기자 명이 있다면 형식 유지 (예: [데일리안 = 기자명] 형식)
               4. 인용문, 수치, 기관명 등은 그럴듯하게 변경
               
               [주의사항]
               - 허위 정보임을 명시하지 말고 자연스럽게 작성
               - 원본의 논조와 비슷하게 유지
               - 적당한 길이로 작성 (원본과 비슷한 분량)
               - 인용문은 자연스럽게 큰따옴표를 사용하되, JSON 형식을 준수하세요
               - 내용 중 개행문자나 백슬래시는 사용하지 마세요
               
               ⚠️ 매우 중요: 반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트나 설명은 포함하지 마세요.
               
               ```json
               {
                 "realNewsId": %s,
                 "title": "생성된 가짜 뉴스 제목",
                 "content": "생성된 가짜 뉴스 본문 전체"
               }
               ```
               """,
                escapeForPrompt(req.title()),
                escapeForPrompt(req.content()),
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
            // 1단계: 응답 정리
            String cleanedResponse = cleanAiResponse(text);

            // 2단계: 파싱 시도
            FakeNewsDto result = objectMapper.readValue(cleanedResponse, FakeNewsDto.class);

            // 3단계: 결과 검증
            validateResult(result);

            return result;

        } catch (Exception e) {
            // 상세한 에러 정보 제공
            throw new ServiceException(500,
                    String.format("가짜 뉴스 생성 실패: %s : %s. 원본 응답: %s",
                            e.getClass().getSimpleName(),
                            e.getMessage(),
                            text.length() > 1000 ? text.substring(0, 1000) + "..." : text));
        }
    }

    /**
     * AI 응답을 정리하여 순수한 JSON만 추출
     */
    private String cleanAiResponse(String response) {
        String cleaned = response.trim();

        // 1. 마크다운 코드 블록 제거
        cleaned = cleaned.replaceAll("(?s)```json\\s*(.*?)\\s*```", "$1");
        cleaned = cleaned.replaceAll("(?s)```\\s*(.*?)\\s*```", "$1");

        // 2. 앞뒤 불필요한 텍스트 제거
        cleaned = cleaned.trim();

        // 3. JSON 시작점과 끝점 찾기
        int jsonStart = cleaned.indexOf('{');
        int jsonEnd = cleaned.lastIndexOf('}');

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
        }

        // 4. 위험한 문자들 치환
        cleaned = cleaned
                .replace("…", "...")        // 줄임표 치환
                .replace("\u2018", "'")     // 왼쪽 스마트 쿼트
                .replace("\u2019", "'")     // 오른쪽 스마트 쿼트
                .replace("\u201C", "\"")    // 왼쪽 스마트 더블 쿼트
                .replace("\u201D", "\"");   // 오른쪽 스마트 더블 쿼트

        cleaned = fixJsonQuotes(cleaned);

        return cleaned.trim();
    }

    /**
     * JSON 내부 문자열 값의 큰따옴표를 안전하게 처리
     */
    private String fixJsonQuotes(String json) {
        try {
            // ObjectMapper를 이용한 사전 검증
            JsonNode jsonNode = objectMapper.readTree(json);

            // 각 필드값을 안전하게 이스케이프하여 새 JSON 생성
            Long realNewsId = jsonNode.get("realNewsId").asLong();
            String title = escapeJsonString(jsonNode.get("title").asText());
            String content = escapeJsonString(jsonNode.get("content").asText());

            return String.format("""
                {
                  "realNewsId": %d,
                  "title": "%s",
                  "content": "%s"
                }
                """, realNewsId, title, content);

        } catch (Exception e) {
            // 파싱 실패 시 기존 방식으로 대체
            return json.replaceAll("(?<!\\\\)\"", "'");
        }
    }
    /**
     * JSON 문자열 내부의 특수문자 이스케이프
     */
    private String escapeJsonString(String text) {
        if (text == null) return "";

        return text
                .replace("\\", "\\\\")  // 백슬래시 먼저 이스케이프
                .replace("\"", "\\\"")  // 큰따옴표 이스케이프 (제거하지 않음!)
                .replace("\n", "\\n")   // 개행 이스케이프
                .replace("\r", "\\r")   // 캐리지 리턴 이스케이프
                .replace("\t", "\\t")   // 탭 이스케이프
                .replace("\b", "\\b")   // 백스페이스 이스케이프
                .replace("\f", "\\f");  // 폼피드 이스케이프
    }

    /**
     * 파싱 결과 검증
     */
    private void validateResult(FakeNewsDto result) {
        if (result.realNewsId() == null) {
            throw new IllegalArgumentException("realNewsId가 누락되었습니다");
        }

        if (result.title() == null || result.title().trim().isEmpty()) {
            throw new IllegalArgumentException("title이 누락되었거나 비어있습니다");
        }

        if (result.content() == null || result.content().trim().isEmpty()) {
            throw new IllegalArgumentException("content가 누락되었거나 비어있습니다");
        }

        // 길이 검증
        if (result.title().length() > 200) {
            throw new IllegalArgumentException("title이 너무 깁니다");
        }

        if (result.content().length() > 10000) {
            throw new IllegalArgumentException("content가 너무 깁니다");
        }
    }

    /**
     * 프롬프트용 문자열 이스케이프
     */
    private String escapeForPrompt(String text) {
        if (text == null) return "";
        return text
                .replace("\"", "'")
                .replace("\\", "")
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}