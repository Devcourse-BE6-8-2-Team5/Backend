package com.back.global.ai.processor;

import com.back.domain.news.common.dto.KeywordGenerationReqDto;
import com.back.domain.news.common.dto.KeywordGenerationResDto;
import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * 뉴스 제목과 본문을 기반 상세 퀴즈 3개를 생성하는 AI 요청 Processor 입니다.
 */
public class KeywordGeneratorProcessor implements AiRequestProcessor<KeywordGenerationResDto> {
    private final KeywordGenerationReqDto req;
    private final ObjectMapper objectMapper;

    public KeywordGeneratorProcessor(KeywordGenerationReqDto req, ObjectMapper objectMapper) {
        this.req = req;
        this.objectMapper = objectMapper;
    }

    // 뉴스 제목과 본문을 바탕으로 퀴즈 생성용 프롬프트 생성 (응답 형식을 JSON 형식으로 작성)
    @Override
    public String buildPrompt() {
        return String.format("""
                Task: 오늘 뉴스 수집을 위한 카테고리별 키워드를 생성하세요.
                
                목적:
                - 네이버 뉴스 검색 API에서 효과적으로 검색되는 키워드 생성
                - 실제 뉴스 제목에 자주 사용되는 실용적인 키워드 선택
                - 각 키워드의 성격에 맞는 정확한 타입 분류

                ⚠️ 중요한 제약사항:
                - 키워드는 뉴스 제목에 실제로 포함될 수 있는 단어여야 합니다
                - 2-4글자의 단순하고 명확한 키워드 (예: "AI", "부동산", "선거")
                - 실제 기자들이 제목에 사용할 법한 키워드를 선택하세요

                현재 날짜: %s

                [키워드 생성 및 타입 분류]
                - 각 카테고리당 정확히 2개의 키워드를 생성합니다
                - 각 키워드의 실제 특성을 분석하여 적절한 타입을 부여합니다
                - 타입 배분을 강제하지 말고, 키워드 자체의 성격에 맞게 판단하세요

                [카테고리별 요구사항]
                - SOCIETY: 사회 이슈 관련 (예: "교육", "복지", "안전", "범죄")
                - ECONOMY: 경제 관련 (예: "금리", "부동산", "주식", "물가")
                - POLITICS: 정치 관련 (예: "국회", "선거", "정책", "여야")
                - CULTURE: 문화/생활 관련 (예: "K팝", "영화", "스포츠", "축제")
                - IT: IT/과학기술 (예: "AI", "반도체", "게임", "5G")

                [키워드 타입 분류 기준]
                각 키워드의 실제 성격을 분석하여 다음 중 하나로 분류:
                - BREAKING: 최근 급부상한 이슈, 긴급 사건, 속보성 키워드
                - ONGOING: 현재 진행 중인 지속적 관심사, 정책 이슈
                - GENERAL: 평상시에도 꾸준히 다뤄지는 일반적 주제
                - SEASONAL: 현재 시기(계절/시점)와 강하게 연관된 키워드

                [제외 키워드]
                다음 키워드들은 최근 과도하게 사용되어 제외해주세요: %s

                ⚠️ 예외: 제외 키워드라도 중대한 새로운 사건이나 완전히 다른 전개가 있다면 포함 가능합니다.

                [시기적 고려사항]
                현재 시기의 특성을 반영하여 계절적 이슈나 연중 주요 일정을 고려해주세요.

                응답 형식:
                반드시 JSON 형식으로만 응답하세요. 각 키워드의 실제 특성에 맞는 타입을 정확히 판단하여 지정하세요.

                ```json
                {
                  "society": [
                    {"keyword": "교육", "keywordType": "GENERAL"},
                    {"keyword": "안전", "keywordType": "ONGOING"}
                  ],
                  "economy": [
                    {"keyword": "금리", "keywordType": "BREAKING"},
                    {"keyword": "부동산", "keywordType": "ONGOING"}
                  ],
                  "politics": [
                    {"keyword": "국회", "keywordType": "ONGOING"},
                    {"keyword": "선거", "keywordType": "SEASONAL"}
                  ],
                  "culture": [
                    {"keyword": "영화", "keywordType": "GENERAL"},
                    {"keyword": "축제", "keywordType": "SEASONAL"}
                  ],
                  "it": [
                    {"keyword": "AI", "keywordType": "BREAKING"},
                    {"keyword": "반도체", "keywordType": "ONGOING"}
                  ]
                }
                ```
                """,
                req.currentDate(),
                req.excludeKeywords() != null ? String.join(", ", req.excludeKeywords()) : "없음");
    }

    // AI 응답을 파싱하여 KeywordGenerationResDto 리스트로 변환
    @Override
    public KeywordGenerationResDto parseResponse(ChatResponse response) {

        String text = response.getResult().getOutput().getText();
        if (text == null || text.trim().isEmpty()) {
            throw new ServiceException(500, "AI 응답이 비어있습니다");
        }

        KeywordGenerationResDto result;
        String cleanedJson = text.replaceAll("(?s)```json\\s*(.*?)\\s*```", "$1").trim();

        try {
            result = objectMapper.readValue(
                    cleanedJson,
                    KeywordGenerationResDto.class
            );
        } catch (Exception e) {
            throw new ServiceException(500, "AI 응답이 JSON 형식이 아닙니다. 응답 : " + cleanedJson);
        }

        validatekeywords(result);

        return result;

    }

    private void validatekeywords(KeywordGenerationResDto result) {
        if(result.society()== null || result.society().size() != 2 ||
           result.economy() == null || result.economy().size() != 2 ||
           result.politics() == null || result.politics().size() != 2 ||
           result.culture() == null || result.culture().size() != 2 ||
           result.it() == null || result.it().size() != 2) {
            throw new ServiceException(500, "각 카테고리당 정확히 2개의 키워드가 필요합니다.");
        }
    }
}
