package com.back.global.ai.processor;

import com.back.domain.news.fake.dto.FakeNewsDto;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;

import java.text.MessageFormat;

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

    @Override
    public String buildPrompt() {
        int contentLength = realNewsDto.content().length();
        String lengthCategory = getLengthCategory(contentLength);
        String formatAnalysis = analyzeFormat(realNewsDto.content());
        String strictLengthGuide = getStrictLengthGuide(contentLength);

        return MessageFormat.format("""
        당신은 가짜 뉴스 창작 전문가입니다. **제목만을 바탕으로** 그럴듯한 가짜 뉴스를 창작하세요.
    
        === 🎯 핵심 원칙 🎯 ===
        **목표**: 제목에 맞는 **완전히 새로운** 뉴스 내용 창작
        - 제목만 보고 **상상으로 내용 작성**
        - 원본 본문은 **참고만** (문체, 형식, 길이)
        - 자연스러운 뉴스 기사 형태로 작성
        - **현실적이고 그럴듯한** 내용으로 창작
        - 독자가 **진짜라고 믿을 만한** 수준으로 작성
        
        === 🚨 분량 준수 필수 🚨 ===
        **원본 분석**:
        - 원본 글자수: {0}자 ({1})
        - **AI는 항상 비슷한 길이로 쓰려는 경향이 있습니다. 이를 극복하세요!**
        - 원본이 짧으면 → 짧게! (길게 쓰지 말것!)
        - 원본이 길면 → 길게! (짧게 줄이지 말것!)
        
        {2}
        
        {3}
        
        === 창작 가이드 ===
        **제목 분석**: "{4}"
        - 주요 키워드 추출하여 내용 구성
        - 관련 업계 상황 고려
        - 협력, 제휴, 발표 등의 맥락 활용
        - **뉴스 성격에 맞는 톤앤매너 적용**
        
        === 원본 스타일 완벽 모방 대상 ===
        {5}
        
        **👆 위 원본을 정확히 분석하여:**
        - **같은 문단 구조** (몇 개 문단인지)
        - **같은 문장 길이 패턴**
        - **같은 정보 밀도** (간결함 vs 상세함)
        - **같은 특수 기호나 형식** (▲, ◆, -, 등)
        - **같은 인용문 스타일과 개수**
        - **같은 마무리 방식**
        
        === 창작 규칙 ===
        1. **제목 기반 창작**: 제목에서 유추할 수 있는 내용으로 자유롭게 창작
        2. **현실적 세부사항**:
          - 구체적인 날짜, 장소, 인물명
          - 현실적인 수치와 데이터
          - 실제 있을 법한 인용문과 발언
          - 관련 업계 정보와 배경
        3. **문체 참고**: 원본 뉴스의 어조와 문체를 완벽 모방
        4. **적절한 길이**: 원본과 **정확히 비슷한** 분량으로 작성
        5. **제목 미포함**: content에는 제목을 절대 포함하지 말고 바로 본문 시작
        6. **앞 제목들 제거**: "현대자동차, 전 직원..." 같은 앞 제목들 절대 포함 금지
        
        === 🚫 절대 금지사항 🚫 ===
        **분량 관련**:
        - 지나치게 간단한 요약식 작성 금지
        - 한두 문장으로 끝내기 금지 (원본이 긴 경우)
        - 중요한 세부사항 생략 금지 (원본이 상세한 경우)
        - 불필요하게 장황하게 늘이기 금지 (원본이 짧은 경우)
        - **천편일률적 분량으로 작성하기 절대 금지**
        
        **내용 관련**:
        - 뻔한 "향후 계획", "기대효과" 마무리 남발 금지
        - 획일적인 "배경-내용-전망" 구조 강요 금지
        - 앞에 붙는 다른 뉴스 제목들 포함 금지
        
        === 특수 형식 모방 가이드 ===
        **원본에 다음이 있으면 반드시 따라하세요:**
        - **▲ 기호** → 동일하게 사용
        - **◆, ●, -** 등 → 동일 위치에 사용
        - **날짜, 시간 표기** → 비슷한 형식으로
        - **괄호 안 설명** → 동일한 스타일로
        - **인용문 형식** → 완전히 동일하게
        - **숫자나 데이터** → 비슷한 구체성으로
        
        === 길이별 절대 규칙 ===
        **🔥 이 규칙을 어기면 실패작입니다:**
        
        **200자 미만**: 핵심 사실만 1-2문단. 설명 최소화. 속보 스타일.
        **200-400자**: 간단한 배경 + 핵심. 2-3문단. 공지사항 스타일.
        **400-800자**: 배경 + 내용 + 반응. 여러 문단. 일반 기사 스타일.
        **800자 이상**: 상세 배경 + 다양한 관점 + 인용문 + 전망. 심층 기사 스타일.
        
        === JSON 작성 규칙 ===
        매우 중요 - 다음 규칙을 절대 위반하지 마세요:
        1. JSON 형식을 정확히 준수한다
        2. content는 반드시 따옴표로 감싸고, **제목 없이 본문만** 포함한다
        3. content 내부 이스케이프 처리:
          - 따옴표: \\\\"
          - 개행: \\\\n
          - 백슬래시: \\\\\\\\
        4. 한글, 영문, 숫자: 그대로 사용 (유니코드 변환 금지)
        5. JSON 외부에 다른 텍스트 추가 금지
        6. 코드 블록(```) 사용 금지
        
        === ✅ 최종 검증 (반드시 확인) ✅ ===
        1. **분량이 원본({0}자)과 정확히 비슷한가?** ← 가장 중요!
        2. **원본과 같은 문단 구조인가?**
        3. **원본의 특수 기호를 따라했는가?**
        4. **제목만 보고 완전히 새로 창작했는가?**
        5. **앞에 붙는 다른 제목들이 포함되지 않았는가?**
        6. **content에 제목이 포함되지 않았는가?**
        7. **개행과 문단 분리가 자연스럽게 적용되었는가?**
        8. **현실적이고 그럴듯한 내용인가?**
        9. **세부사항이 충분히 포함되었는가?** (원본이 상세한 경우)
        10. **JSON 형식이 정확한가?**
        
        === 응답 형식 ===
        '{'
         "content": "원본 {0}자와 동일한 분량, 동일한 구조로 창작된 본문 (제목과 앞 제목들 제외)"
        '}'
        """,
                contentLength,                           // {0} - 원본 글자수
                lengthCategory,                          // {1} - 길이 카테고리
                formatAnalysis,                          // {2} - 형식 분석
                strictLengthGuide,                       // {3} - 엄격한 길이 가이드
                cleanText(realNewsDto.title()),          // {4} - 제목
                cleanText(realNewsDto.content())         // {5} - 원본 내용
        );
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
                .replace("%", "%%")           // % -> %% 이스케이프
                .replace("\\", "\\\\")        // \ -> \\ 이스케이프
                .replaceAll("\\s+", " ")
                .trim();
    }
    /**
     * 결과를 FakeNewsDto로 변환
     */
    private FakeNewsDto convertToFakeNewsDto(FakeNewsGeneratedRes result) {
        if (result.content() == null || result.content().trim().isEmpty()) {
            throw new ServiceException(500, "AI 응답에 content가 누락되었습니다");
        }

        return FakeNewsDto.of(realNewsDto.id(), result.content());
    }
    /**
     * AI 응답 파싱용 내부 레코드
     */
    private record FakeNewsGeneratedRes(
            @JsonProperty("content") String content
    ) {}

    private String getLengthCategory(int length) {
        if (length < 200) return "극짧은 뉴스";
        else if (length < 400) return "짧은 뉴스";
        else if (length < 800) return "중간 뉴스";
        else return "긴 뉴스";
    }

    private String analyzeFormat(String content) {
        if (content == null) return "";

        StringBuilder analysis = new StringBuilder("**원본 형식 분석**:\n");

        // 특수 기호 분석
        if (content.contains("▲")) analysis.append("- ▲ 기호 포함 → 동일하게 사용할 것\n");
        if (content.contains("◆")) analysis.append("- ◆ 기호 포함 → 동일하게 사용할 것\n");
        if (content.contains("●")) analysis.append("- ● 기호 포함 → 동일하게 사용할 것\n");
        if (content.contains("-")) analysis.append("- - 기호 포함 → 동일하게 사용할 것\n");

        // 문단 분석
        String[] paragraphs = content.split("\\n\\s*\\n");
        analysis.append("- 문단 수: ").append(paragraphs.length).append("개 → 동일하게 구성할 것\n");

        // 인용문 분석
        long quoteCount = content.chars().filter(ch -> ch == '"' || ch == '"' || ch == '"').count() / 2;
        if (quoteCount > 0) {
            analysis.append("- 인용문 ").append(quoteCount).append("개 → 동일한 개수로 포함할 것\n");
        }

        // 숫자/데이터 분석
        if (content.matches(".*\\d+%.*")) analysis.append("- 퍼센트 데이터 포함 → 비슷한 형식으로\n");
        if (content.matches(".*\\d+억.*")) analysis.append("- 억 단위 숫자 포함 → 비슷한 규모로\n");
        if (content.matches(".*\\d+만.*")) analysis.append("- 만 단위 숫자 포함 → 비슷한 규모로\n");

        return analysis.toString();
    }

    private String getStrictLengthGuide(int length) {
        if (length < 200) {
            return """
        **🔥 극짧은 뉴스 작성법 (200자 미만)**:
        - 1-2문장으로 핵심만! 
        - 배경설명 금지
        - "발표했다", "밝혔다" 등 간단한 서술
        - 속보나 단신 형태
        - 절대 길게 쓰지 말 것!
        """;
        } else if (length < 400) {
            return """
        **📝 짧은 뉴스 작성법 (200-400자)**:
        - 2-3문장으로 구성
        - 간단한 배경 + 핵심 사실
        - 인용문 최대 1개
        - 공지사항이나 발표문 형태
        - 중간 길이로 유지!
        """;
        } else if (length < 800) {
            return """
        **📰 중간 뉴스 작성법 (400-800자)**:
        - 2-3문단으로 구성
        - 배경 + 핵심내용 + 반응
        - 인용문 2-3개 적절히
        - 일반적인 기사 형태
        - 적당한 분량 유지!
        """;
        } else {
            return """
        **📚 긴 뉴스 작성법 (800자 이상)**:
        - 3-4문단으로 상세 구성
        - 상세 배경 + 다양한 관점
        - 여러 관계자 인용문
        - 구체적 데이터와 분석
        - 충분히 길게 작성!
        """;
        }
    }
}