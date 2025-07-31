package com.back.global.ai.processor;

import com.back.domain.news.fake.dto.FakeNewsDto;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * 진짜 뉴스를 기반으로 가짜 뉴스를 생성하는 AI 요청 Processor 입니다.
 */
@Slf4j
public class FakeNewsGeneratorProcessor implements AiRequestProcessor<FakeNewsDto> {
    private final RealNewsDto realNewsDto;
    private final ObjectMapper objectMapper;

    public FakeNewsGeneratorProcessor(RealNewsDto realNewsDto, ObjectMapper objectMapper) {
        this.realNewsDto = realNewsDto;
        this.objectMapper = objectMapper;
    }

    // 진짜 뉴스를 바탕으로 가짜 뉴스 생성용 프롬프트 생성
    @Override
    public String buildPrompt() {
        return String.format("""
           당신은 가짜 뉴스 창작 전문가입니다. **제목만을 바탕으로** 그럴듯한 가짜 뉴스를 창작하세요.
           
           === 핵심 원칙 ===
           **목표**: 제목에 맞는 **완전히 새로운** 뉴스 내용 창작
           - 제목만 보고 **상상으로 내용 작성**
           - 원본 본문은 **참고만** (문체, 형식, 길이)
           - **현실적이고 그럴듯한** 내용으로 창작
           - 독자가 **진짜라고 믿을 만한** 수준으로 작성

           === 창작 규칙 ===
           1. **제목 기반 창작**: 제목에서 유추할 수 있는 내용으로 자유롭게 창작
           2. **현실적 세부사항**:\s
              - 구체적인 날짜, 장소, 인물명
              - 현실적인 수치와 데이터
              - 실제 있을 법한 인용문과 발언
              - 관련 업계 정보와 배경
           3. **문체 참고**: 원본 뉴스의 어조와 문체를 모방
           4. **적절한 길이**: 원본과 비슷한 분량으로 작성
           5. **제목 미포함**: content에는 제목을 절대 포함하지 말고 바로 본문 시작

           === 개행 및 문단 구성 ===
           **자연스러운 뉴스 기사 형태로 작성:**
           - 첫 문단: 핵심 사실 요약
           - 중간 문단들: 구체적 내용, 배경, 인용문 등
           - 마지막 문단: 향후 계획이나 의미
           - 문단 사이: \\\\n\\\\n으로 구분
           - 같은 문단 내: 관련 문장들 자연스럽게 연결

           === 창작 가이드 ===
           **제목 분석**: "%s"
           - 주요 키워드 추출하여 내용 구성
           - 관련 업계 상황 고려
           - 협력, 제휴, 발표 등의 맥락 활용

           **참고용 원본 문체**:
           %s
           
           **중요: 반드시 정확한 ID 사용**
           realNewsId는 반드시 %s을 사용해야 합니다. 다른 숫자를 사용하지 마세요!
           

           === JSON 작성 규칙 ===
           매우 중요 - 다음 규칙을 절대 위반하지 마세요:
           1. JSON 형식을 정확히 준수한다
           2. realNewsId는 따옴표 없는 숫자로 입력한다
           3. content는 반드시 따옴표로 감싸고, **제목 없이 본문만** 포함한다
           4. content 내부 이스케이프 처리:
              - 따옴표: \\\\"
              - 개행: \\\\n
              - 백슬래시: \\\\\\\\
           5. 한글, 영문, 숫자: 그대로 사용 (유니코드 변환 금지)
           6. JSON 외부에 다른 텍스트 추가 금지
           7. 코드 블록(```) 사용 금지

           === 최종 검증 ===
           생성 전 반드시 확인하세요:
           1. realNewsId가 정확히 %s인가?
           2. 제목에 맞는 내용으로 창작했는가?
           3. content에 제목이 포함되지 않았는가?
           4. 개행이 자연스럽게 적용되었는가?
           5. 현실적이고 그럴듯한 내용인가?
           6. JSON 형식이 정확한가??

           === 응답 형식 ===
           {
             "realNewsId": %s,
             "content": "제목을 바탕으로 창작한 가짜 뉴스 본문"
           }
           """,
                cleanText(realNewsDto.title()),
                cleanText(realNewsDto.content()),
                realNewsDto.id(),
                realNewsDto.id(),
                realNewsDto.id(),
                realNewsDto.id());
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
            FakeNewsGeneratedRes result = objectMapper.readValue(cleanedJson, FakeNewsGeneratedRes.class);

            return convertToFakeNewsDto(result);

        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패: {}", e.getMessage());
            throw new ServiceException(500, "AI 응답의 JSON 형식이 올바르지 않습니다: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("데이터 변환 실패: {}", e.getMessage());
            throw new ServiceException(500, "AI 응답 데이터 변환에 실패했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("예상치 못한 오류: {}", e.getMessage());
            throw new ServiceException(500, "AI 응답 처리 중 예상치 못한 오류가 발생했습니다: " + e.getMessage());
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
     * 결과를 FakeNewsDto로 변환
     */
    private FakeNewsDto convertToFakeNewsDto(FakeNewsGeneratedRes result) {
        // 기본 유효성 검증
        if (result.realNewsId() == null) {
            throw new ServiceException(500, "AI 응답에 realNewsId가 누락되었습니다");
        }

        if (result.content() == null || result.content().trim().isEmpty()) {
            throw new ServiceException(500, "AI 응답에 content가 누락되었습니다");
        }

        // ID 일치 확인
        if (!result.realNewsId().equals(realNewsDto.id())) {
            log.warn("realNewsId 불일치: 예상 {}, 실제 {}", realNewsDto.id(), result.realNewsId());
        }

        return FakeNewsDto.of(result.realNewsId(), result.content());
    }
    /**
     * AI 응답 파싱용 내부 레코드
     */
    private record FakeNewsGeneratedRes(
            @JsonProperty("realNewsId") Long realNewsId,
            @JsonProperty("content") String content
    ) {}

}