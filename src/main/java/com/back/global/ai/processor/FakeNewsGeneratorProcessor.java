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

    @Override
    public String buildPrompt() {
        int contentLength = realNewsDto.content().length();
        String lengthCategory = getLengthCategory(contentLength);
        String strictLengthGuide = getStrictLengthGuide(contentLength);
        String cleanTitle = cleanText(realNewsDto.title());
        String cleanContent = cleanText(realNewsDto.content());

        // 분량별 구체적 가이드 생성
        String specificLengthGuide = generateSpecificLengthGuide(contentLength);
        String sentenceCountGuide = generateSentenceCountGuide(contentLength);

        return String.format("""
            당신은 가짜 뉴스 창작 전문가입니다. **제목만을 바탕으로** 그럴듯한 가짜 뉴스를 창작하세요.
        
            ⚠️ **최우선 임무: 정확한 분량 맞추기** ⚠️
            원본 분량: %d자 → 반드시 %d자 ± 50자 이내로 작성!
            
            %s
            
            %s
        
            === 🎯 3단계 창작 프로세스 🎯 ===
            
            **1단계: 분량 계획 수립**
            - 목표 글자수: %d자 (%s)
            - 필요 문장수: %s
            - 문단 구성: %s
            - ❌ 이 단계를 건너뛰면 100%% 실패합니다!
            
            **2단계: 내용 창작**
            - 제목 분석: "%s"
            - 원본 스타일 참고 (아래 참조)
            - 현실적 세부사항 포함 (날짜, 장소, 인물, 수치)
            - **매 문장마다 분량을 의식하며 작성**
            
            **3단계: 분량 검증**
            - 작성 완료 후 반드시 글자수 확인
            - %d자와 비교하여 ±50자 이내인지 점검
            - 부족하면 세부사항 추가, 초과하면 불필요한 부분 제거
            
            === 🚨 분량별 절대 규칙 🚨 ===
            %s
            
            === ⭐ 분량 맞추기 비법 ⭐ ===
            **너무 짧을 때 늘리는 방법:**
            - 구체적 날짜/시간 추가 ("지난 15일 오후 2시")
            - 정확한 장소명 추가 ("서울 강남구 테헤란로 소재")
            - 관계자 발언 인용문 추가
            - 배경 설명 1-2문장 추가
            - 관련 업계 현황 언급
            
            **너무 길 때 줄이는 방법:**
            - 불필요한 수식어 제거
            - 중복 설명 통합
            - 부가적 배경 설명 축소
            - 예상 효과 등 추측성 내용 제거
            
            === 원본 스타일 완벽 모방 ===
            **분석 대상:**
            %s
            
            **필수 모방 요소:**
            - 문단 수: 원본과 동일하게
            - 문장 길이: 원본 패턴 따라하기
            - 특수 기호: ▲, ◆, -, () 등 동일 사용
            - 인용문 형식: 원본과 같은 스타일
            - 마무리 방식: 원본과 동일한 톤
            
            === 🔥 절대 금지사항 🔥 ===
            1. **분량 무시하고 창작하기** - 가장 큰 실패 요인!
            2. **제목을 content에 포함하기** - 절대 금지!
            3. **앞에 붙는 다른 제목들 포함하기** - 절대 금지!
            4. **원본 제목 그대로 복사하기** - 절대 금지!
            5. 천편일률적인 "향후 계획" 마무리
            6. 원본 내용 그대로 복사하기
            7. 비현실적이거나 과장된 내용
            8. %d자를 크게 벗어나는 분량
            9. **\\n 같은 이스케이프 문자 그대로 출력하기**
            
            === 💡 중요한 작성 원칙 💡 ===
            - content는 **바로 본문부터 시작**합니다
            - 제목이나 헤더는 절대 포함하지 마세요
            - 첫 문장부터 바로 뉴스 내용으로 시작하세요
            - 문단 구분은 자연스러운 개행으로 처리하세요
            - JSON 외부에 다른 텍스트 추가 금지
            - 코드 블록(```) 사용 금지
            - 설명이나 주석 추가 금지
            
            === JSON 출력 규칙 ===
            반드시 다음 형식으로만 응답:
            {
             "content": "정확히 %d자 ± 50자 이내의 본문만"
            }
            
            **이스케이프 처리:**
            - 내부 따옴표: \\\\" (백슬래시 + 따옴표)
            - 개행 문자: \\\\n (백슬래시 + n)  // ← 이렇게 수정
            - 백슬래시: \\\\\\\\ (백슬래시 + 백슬래시)
            - 작은따옴표는 그대로 사용
            - 한글, 영문, 숫자: 그대로 사용 (유니코드 변환 금지)
            - 특수문자, 이모지: 그대로 사용 (이스케이프 금지)
            
            === ✅ 최종 점검표 ✅ ===
            응답 전 반드시 확인:
            □ 글자수가 %d자 ± 50자 이내인가?
            □ 원본과 같은 문단 구조인가?
            □ **제목이 content에 절대 포함되지 않았는가?**
            □ **첫 문장부터 바로 본문 내용인가?**
            □ 현실적이고 그럴듯한 내용인가?
            □ 원본 스타일을 잘 모방했는가?
            □ JSON 형식이 정확한가?
            □ **\\n 같은 이스케이프 문자가 그대로 출력되지 않았는가?**
            
            **마지막 경고: 분량을 맞추지 못하면 무조건 실패작입니다!**
            **제목을 포함하면 무조건 실패작입니다!**
            """,
                contentLength, contentLength,  // 분량 강조
                specificLengthGuide,
                strictLengthGuide,
                contentLength, lengthCategory,  // 1단계
                sentenceCountGuide,
                getStructureGuide(contentLength),
                cleanTitle,  // 2단계
                contentLength,  // 3단계
                getLengthSpecificRules(contentLength),  // 분량별 규칙
                cleanContent,  // 원본 스타일
                contentLength,  // 금지사항
                contentLength,  // JSON 출력
                contentLength   // 최종 점검
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
    private String generateSpecificLengthGuide(int length) {
        if (length < 200) {
            return "🎯 **초단문 작성법**: 핵심만! 1-2개 문장으로 간결하게!";
        } else if (length < 400) {
            return "🎯 **단문 작성법**: 배경 1문장 + 핵심 2-3문장 + 마무리 1문장";
        } else if (length < 800) {
            return "🎯 **중문 작성법**: 도입-전개-결론 구조로 균형있게 배분";
        } else {
            return "🎯 **장문 작성법**: 상세한 배경, 다양한 관점, 인용문 포함하여 풍부하게";
        }
    }

    // 문장 수 가이드 생성
    private String generateSentenceCountGuide(int length) {
        int sentences = Math.max(1, length / 80); // 평균 80자당 1문장
        return String.format("약 %d-%d개 문장 필요", sentences - 1, sentences + 1);
    }

    // 구조 가이드 생성
    private String getStructureGuide(int length) {
        if (length < 200) return "1개 문단";
        else if (length < 400) return "2개 문단";
        else if (length < 800) return "3-4개 문단";
        else return "4-5개 문단";
    }

    // 분량별 구체적 규칙
    private String getLengthSpecificRules(int length) {
        if (length < 200) {
            return """
                    **200자 미만 규칙:**
                    - 핵심 사실만 담기
                    - 배경 설명 최소화
                    - 1-2개 문단으로 완결
                    - 인용문 1개 이하
                    """;
        } else if (length < 400) {
            return """  
                    **200-400자 규칙:**
                    - 간단한 배경 + 핵심 내용
                    - 2-3개 문단 구성
                    - 인용문 1-2개 포함
                    - 구체적 수치 1-2개 포함
                    """;
        } else if (length < 800) {
            return """
                    **400-800자 규칙:**
                    - 배경-내용-반응/전망 구조
                    - 3-4개 문단 구성
                    - 인용문 2-3개 포함
                    - 관련 업계 상황 언급
                    """;
        } else {
            return """
                    **800자 이상 규칙:**
                    - 상세한 배경과 다각도 분석
                    - 4-5개 문단 구성
                    - 다양한 인용문과 데이터
                    - 향후 전망까지 포함
                    """;
        }
    }
}