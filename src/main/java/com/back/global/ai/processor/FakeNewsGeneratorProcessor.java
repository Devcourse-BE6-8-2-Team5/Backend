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
            
            === 분량 및 구조 요구사항 ===
            **적절한 분량**: 원본 뉴스와 **균형 잡힌 길이**로 작성하세요
            - 원본 글자수: %d자
            - 원본이 길면 → 상세하게, 짧으면 → 간결하게
            - **너무 짧게 요약하지도, 불필요하게 길게 늘이지도 말 것**
            
            **구조 분석 및 모방**:
            - 원본 문단 수: 그대로 유지
            - 각 문단별 길이: 원본과 비슷하게
            - 세부 정보 밀도: 원본 수준으로 풍부하게
            - 인용문/발언: 원본에 있는 만큼 포함
            - 수치/데이터: 원본처럼 구체적으로 제시
            
            === 창작 규칙 ===
            1. **제목 기반 창작**: 제목에서 유추할 수 있는 내용으로 자유롭게 창작
            2. **현실적 세부사항**:
              - 구체적인 날짜, 장소, 인물명
              - 현실적인 수치와 데이터
              - 실제 있을 법한 인용문과 발언
              - 관련 업계 정보와 배경
            3. **문체 참고**: 원본 뉴스의 어조와 문체를 모방
            4. **적절한 길이**: 원본과 비슷한 분량으로 작성
            5. **제목 미포함**: content에는 제목을 절대 포함하지 말고 바로 본문 시작
            
            === 분량 확보 전략 ===
            **각 문단별 충실도 확보**:
            1. **첫 문단**: 핵심 사실 + 구체적 배경
            2. **중간 문단들**:
              - 상세한 내용 설명
              - 관련자 인용문 포함
              - 구체적 수치와 데이터
              - 업계 현황과 의미
            3. **마지막 문단**: 향후 계획 + 기대효과 상세 서술
            
            **절대 하지 말 것**:
            - 지나치게 간단한 요약식 작성 금지
            - 한두 문장으로 끝내기 금지
            - 중요한 세부사항 생략 금지
            - 불필요하게 장황하게 늘이기 금지
            
            === 개행 및 문단 구성 ===
            **자연스러운 뉴스 기사 형태로 작성:**
            - 첫 문단: 핵심 사실 요약 + 배경
            - 중간 문단들: 구체적 내용, 배경, 인용문, 데이터 등
            - 마지막 문단: 향후 계획이나 의미
            - 문단 사이: 자연스러운 개행으로 구분
            - 같은 문단 내: 관련 문장들 자연스럽게 연결
            
            === 창작 가이드 ===
            **제목 분석**: "%s"
            - 주요 키워드 추출하여 내용 구성
            - 관련 업계 상황 고려
            - 협력, 제휴, 발표 등의 맥락 활용
            
            **참고용 원본 문체 및 분량**:
            %s
            

            **분량 달성 체크리스트**:
            ✓ 원본(%d자)과 적절히 균형 잡힌 분량인가?
            ✓ 각 문단이 충분한 정보를 담고 있는가?
            ✓ 인용문과 구체적 데이터를 적절히 포함했는가?
            ✓ 배경 설명과 향후 계획이 자연스럽게 포함되었는가?
            
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
            
            === 최종 검증 ===
            생성 전 반드시 확인하세요:
            1. 제목에 맞는 내용으로 창작했는가?
            2. content에 제목이 포함되지 않았는가?
            3. **분량이 원본과 비슷한가? (최소 %d자 이상)**
            4. 개행과 문단 분리가 자연스럽게 적용되었는가?
            5. 현실적이고 그럴듯한 내용인가?
            6. 세부사항이 충분히 포함되었는가?
            7. JSON 형식이 정확한가?
            
            === 응답 형식 ===
            {
             "content": "제목을 바탕으로 창작한 가짜 뉴스 본문 (원본과 비슷한 분량으로)"
            }
            """,
                realNewsDto.content().length(),              // 원본 글자수
                cleanText(realNewsDto.title()),              // 제목
                cleanText(realNewsDto.content()),            // 원본 내용
                realNewsDto.content().length(),              // 분량 체크용
                realNewsDto.content().length()
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

}