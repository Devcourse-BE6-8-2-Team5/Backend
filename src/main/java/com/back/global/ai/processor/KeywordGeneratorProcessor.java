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
                - 카테고리별 균형 잡힌 키워드 배분
                
                ⚠️ 중요한 제약사항:
                - 키워드는 뉴스 제목에 실제로 포함될 수 있는 단어여야 합니다
                - 너무 길거나 복잡한 구문은 피해주세요 (2-4글자 권장)
                - 실제 기자들이 제목에 사용할 법한 키워드를 선택하세요
                - 추상적 개념보다는 구체적이고 검색 가능한 단어 우선
                
                현재 상황 분석 및 고려사항:
                현재 날짜: %s
                
                [키워드 생성 전략]
                - 각 카테고리당 정확히 2개의 키워드를 생성합니다
                - 키워드는 단순하고 명확해야 합니다 (예: "AI", "부동산", "선거")
                - 뉴스 제목에 자주 등장하는 핵심 단어 위주로 선택
                - 검색 효율성이 높은 키워드 우선
                
                [카테고리별 키워드 예시 및 요구사항]
                - SOCIETY: 사회 이슈 관련 (예: "교육", "복지", "안전", "범죄")
                  * 사회 뉴스 제목에 자주 나오는 핵심 단어
                
                - ECONOMY: 경제 관련 (예: "금리", "부동산", "주식", "물가")  
                  * 경제 뉴스 제목의 필수 키워드들
                
                - POLITICS: 정치 관련 (예: "국회", "선거", "정책", "여야")
                  * 정치 뉴스에서 빈번히 사용되는 용어
                
                - CULTURE: 문화/생활 관련 (예: "K팝", "영화", "스포츠", "축제")
                  * 문화 뉴스 제목에 등장하는 친숙한 단어
                
                - IT: IT/과학기술 (예: "AI", "반도체", "게임", "5G")
                  * IT 뉴스에서 자주 보이는 기술 용어
                
                [제외 키워드 및 중복 방지]
                다음 키워드들은 최근 과도하게 사용되어 제외해주세요: %s
                
                ⚠️ 중요: 제외 키워드라도 다음의 경우는 예외적으로 포함 가능합니다:
                - 해당 키워드와 관련된 중대한 새로운 사건이 발생한 경우
                - 기존과 완전히 다른 관점이나 전개가 있는 경우
                - 국가적/사회적으로 매우 중요한 이슈인 경우
                
                [시즌/시기별 고려사항]
                현재 시기의 특성을 반영하여 다음을 고려해주세요:
                - 계절적 이슈 (겨울: 난방, 에너지 / 여름: 휴가, 폭염 등)
                - 연중 주요 일정 (예산, 국정감사, 주요 행사 등)
                - 최근 사회적 관심 급상승 주제들
                
                응답 형식:
                응답은 반드시 아래 필드들을 포함한 JSON 형식으로만 작성하세요. 각 키워드마다 타입도 함께 지정해주세요.
                키워드 타입:
                - BREAKING: 속보성 (긴급한 이슈, 사건사고)
                - ONGOING: 진행형 (지속적인 관심사, 정책 이슈)  
                - GENERAL: 일반적 (꾸준한 관심사)
                - SEASONAL: 계절성 (시기적 특성)
                
                설명 없이 JSON만 응답하세요.
                ```json
                {
                  "society": [
                    {"keyword": "키워드1", "type": "ONGOING"},
                    {"keyword": "키워드2", "type": "GENERAL"}
                  ],
                  "economy": [
                    {"keyword": "키워드1", "type": "BREAKING"},
                    {"keyword": "키워드2", "type": "ONGOING"}
                  ],
                  "politics": [
                    {"keyword": "키워드1", "type": "ONGOING"},
                    {"keyword": "키워드2", "type": "GENERAL"}
                  ],
                  "culture": [
                    {"keyword": "키워드1", "type": "SEASONAL"},
                    {"keyword": "키워드2", "type": "GENERAL"}
                  ],
                  "it": [
                    {"keyword": "키워드1", "type": "BREAKING"},
                    {"keyword": "키워드2", "type": "ONGOING"}
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
