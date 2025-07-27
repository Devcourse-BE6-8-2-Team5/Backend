package com.back.global.ai.processor;

import com.back.domain.news.common.entity.KeywordGenerationReqDto;
import com.back.domain.news.common.entity.KeywordGenerationResDto;
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
                - 질 높은 뉴스를 수집하기 위한 효과적인 검색 키워드 생성
                - 카테고리별 균형 잡힌 키워드 배분
                - 시의성과 대중 관심도를 반영한 키워드 선택
                - 중요한 진행형 이슈와 새로운 사건을 놓치지 않는 키워드 선택
                
                현재 상황 분석 및 고려사항:
                현재 날짜: %s
                
                [상황 인식 기반 키워드 우선순위]
                1. 긴급/속보성 이슈: 최근 발생한 중요 사건의 후속 전개
                2. 진행형 주요 이슈: 지속적으로 관심받는 사회적 이슈들
                3. 정책/제도 변화: 새로운 정책 발표나 제도 개편
                4. 국제적 관심사: 글로벌 이슈 중 국내 영향이 있는 것들
                5. 교육적 가치: 국민들이 알아야 할 중요한 정보
                
                [키워드 생성 전략]
                - 각 카테고리당 정확히 2개의 키워드를 생성합니다
                - 키워드는 구체적이고 검색 효율성이 높아야 합니다
                - 단순 반복보다는 새로운 관점이나 전개를 다룰 수 있는 키워드 우선
                - 클릭베이트성 키워드보다는 실질적 정보가 있는 뉴스를 찾을 수 있는 키워드
                
                [카테고리별 세부 요구사항]
                - SOCIETY: 사회 이슈, 사건사고, 사회 정책, 사회 현상 관련
                  * 우선순위: 최근 사회적 관심사, 새로운 사회 문제, 정책 변화
                
                - ECONOMY: 경제 정책, 시장 동향, 기업 활동, 경제 지표 관련
                  * 우선순위: 경제 정책 변화, 시장 이슈, 산업 동향
                
                - POLITICS: 정치적 사건, 정책 발표, 정치 이슈, 선거 관련
                  * 우선순위: 정책 발표, 정치적 쟁점, 제도 개편
                
                - CULTURE: 문화 트렌드, 예술, 엔터테인먼트, 교육, 라이프스타일 관련
                  * 우선순위: 새로운 문화 현상, 교육 이슈, 사회적 트렌드
                
                - IT: 기술 혁신, IT 산업, 디지털 트렌드, 과학 기술 관련
                  * 우선순위: 기술 혁신, 디지털 정책, IT 산업 동향
                
                [제외 키워드 및 중복 방지]
                다음 키워드들은 최근 과도하게 사용되어 제외해주세요: %s
                
                ⚠️ 중요: 제외 키워드라도 다음의 경우는 예외적으로 포함 가능합니다:
                - 해당 키워드와 관련된 중대한 새로운 사건이 발생한 경우
                - 기존과 완전히 다른 관점이나 전개가 있는 경우
                - 국가적/사회적으로 매우 중요한 이슈인 경우
                
                [시즌/시기별 고려사항]
                현재 시기의 특성을 반영하여 다음을 고려해주세요:
                - 계절적 이슈 (겨울철: 난방, 에너지 / 봄철: 신학기, 취업 등)
                - 연중 주요 일정 (예산 심의, 정기국회, 주요 행사 등)
                - 최근 사회적 관심 급상승 주제들
                
                응답 형식:
                응답은 반드시 아래 필드들을 포함한 JSON 형식으로만 작성하세요. 설명 없이 JSON만 응답하세요.
                ```json
                {
                  "society": ["키워드1", "키워드2"],
                  "economy": ["키워드1", "키워드2"],
                  "politics": ["키워드1", "키워드2"],
                  "culture": ["키워드1", "키워드2"],
                  "it": ["키워드1", "키워드2"]
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
