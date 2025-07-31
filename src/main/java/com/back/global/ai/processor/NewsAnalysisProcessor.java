package com.back.global.ai.processor;

import com.back.domain.news.common.dto.AnalyzedNewsDto;
import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.List;

@Slf4j
public class NewsAnalysisProcessor implements AiRequestProcessor<List<AnalyzedNewsDto>> {

    private final List<RealNewsDto> newsToAnalyze;
    private final ObjectMapper objectMapper;

    public NewsAnalysisProcessor(List<RealNewsDto> newsToAnalyze, ObjectMapper objectMapper) {
        if (newsToAnalyze == null || newsToAnalyze.isEmpty()) {
            throw new ServiceException(400, "분석할 뉴스가 제공되지 않았습니다");
        }

        this.newsToAnalyze = newsToAnalyze;
        this.objectMapper = objectMapper;
    }

    @Override
    public String buildPrompt() {
        StringBuilder newsInput = new StringBuilder();
        for (int i = 0; i < newsToAnalyze.size(); i++) {
            RealNewsDto news = newsToAnalyze.get(i);
            newsInput.append("뉴스 ").append(i + 1).append(":\n\n")
                    .append("내용: ").append(cleanText(news.content()))
                    .append("\n---\n");
        }

        return String.format("""
                당신은 뉴스 분석 전문가입니다. 다음 3가지 작업을 동시에 수행하되, 각각의 중요도에 맞게 처리하라:
                
                === 작업 1: 품질 평가 (가중치 40점) ===
                [신뢰도 분석] 다음 기준에 따라 각 뉴스의 품질을 평가하고 점수를 매겨라 (0-100점):
                - 출처 명확성 (25점): 구체적 출처나 발언자 명시 여부
                - 사실 검증성 (25점): 확인 가능한 구체적 정보 포함 여부
                - 객관성 (25점): 편향되지 않은 중립적 서술인지
                - 완성도 (25점): 5W1H 정보 충실성
                
                === 작업 2: 카테고리 분류 (가중치 20점) ===
                [키워드 기반 분류] 다음 카테고리 중 하나로 분류하라:
                - POLITICS: 정부, 국회, 정책, 선거, 정치인
                - ECONOMY: 기업, 주식, 금리, GDP, 산업
                - SOCIETY: 사건사고, 복지, 교육, 환경
                - CULTURE: 문화, 예술, 스포츠, 연예
                - IT: 기술, 인터넷, AI, 디지털
                
                === 작업 3: 불필요한 메타데이터 삭제 (가중치 40점) ===
                [불필요한 항목 삭제] 다음 메타데이터를 제거하라:
                - 기자 이름 및 이메일 (예: "홍길동 기자", "gil@example.com")
                - 이미지 캡션 및 설명 (예: "[사진]", "이미지 출처", "사진=OO일보")
                - 뉴스 발행일, 수정일 등 날짜 정보
                - 본문과 무관한 홍보 문구, 링크, SNS 공유 안내
                - 괄호로 처리된 메타정보 (예: "(서울=연합뉴스)")
                
                ---
                매우 중요:
                - 이미 포함된 개행은 삭제하지 않는다.
                - 각 뉴스 본문 내용은 최대한 유지한다.
                - 줄바꿈은 실제 줄바꿈 문자("\\n")를 사용한다.
                - 문단이 바뀔 때는 문장 사이에 빈 줄(두 줄 개행)을 반드시 넣어 시각적으로 구분한다.
                - 한 문단 내에서는 문장들을 붙여서 한 줄 개행으로 연결하되, 문장이 너무 길거나 내용상 자연스럽게 나누는 경우에만 적절히 개행해도 무방하다. \s
                - 너무 자주 개행하지 말고, 실제 뉴스 기사 형태처럼 의미 단위별 적절한 간격을 유지하라.
                - 모든 문장이 붙어서 나오지 않도록 가독성을 최우선으로 고려한다.
                
                작업 완료 후 다음 항목을 자체 점검하라:
                
                1. JSON 구조가 정확히 맞는가? (newsIndex, qualityScore, category, cleanedContent 필드가 모두 포함)
                2. category 값은 사전에 주어진 5개 카테고리 중 하나인가? (POLITICS, ECONOMY, SOCIETY, CULTURE, IT)
                3. cleanedContent에 적절한 개행과 단락 구분이 포함되어 있는가?
                4. 편향적 표현이나 주관적 판단이 없는가?
                5. 불필요한 메타데이터가 삭제되어 있는가?
                
                검증 실패 시 해당 부분을 수정하여 최종 응답을 생성하라.
                
                응답은 반드시 다음 JSON 구조를 정확히 따라야 한다. 이 외의 정보는 포함하지 않는다.:
                
                [
                  {
                    "newsIndex": 1,
                    "qualityScore": 85,
                    "category": "POLITICS",
                    "cleanedContent": "정제된 뉴스 본문 내용"
                  },
                  {
                    "newsIndex": 2,
                    "qualityScore": 72,
                    "category": "ECONOMY",
                    "cleanedContent": "정제된 뉴스 본문 내용"
                  }
                ]
                
                분석할 뉴스:
                %s
                """, newsInput);
    }


    @Override
    public List<AnalyzedNewsDto> parseResponse(ChatResponse response) {
        String text = response.getResult().getOutput().getText();
        if (text == null || text.trim().isEmpty()) {
            throw new ServiceException(500, "AI 응답이 비어있습니다");
        }

        log.debug("AI 응답 길이: {}", text.length());

        try {
            String cleanedJson = cleanResponse(text);

            List<NewsAnalyzedRes> results = objectMapper.readValue(
                    cleanedJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NewsAnalyzedRes.class)
            );

            return convertToFilteredNewsDto(results);

        }  catch (JsonProcessingException e) {
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

    //  AI 응답 정리 - 마크다운 코드 블록 제거
    private String cleanResponse(String text) {
        return text.trim()
                .replaceAll("(?s)```json\\s*(.*?)\\s*```", "$1")
                .replaceAll("```", "")
                .trim();
    }

    //  프롬프트용 텍스트 정리
    private String cleanText(String text) {
        if (text == null) return "";
        return text.replace("\"", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }

    //  결과를 FilteredNewsDto로 변환
    private List<AnalyzedNewsDto> convertToFilteredNewsDto(List<NewsAnalyzedRes> results) {
        if (results.size() != newsToAnalyze.size()) {
            throw new ServiceException(500,
                    String.format("결과 개수 불일치: 요청 %d개, 응답 %d개", newsToAnalyze.size(), results.size()));
        }

        return results.stream().map(result -> {
            int index = result.newsIndex() - 1; // 1-based to 0-based
            RealNewsDto originalNews = newsToAnalyze.get(index);

            // 카테고리가 적용된 새로운 RealNewsDto 생성
            RealNewsDto updatedNews = RealNewsDto.of(
                    originalNews.id(),
                    originalNews.title(),
                    result.cleanedContent(),
                    originalNews.description(),
                    originalNews.link(),
                    originalNews.imgUrl(),
                    originalNews.originCreatedDate(),
                    originalNews.mediaName(),
                    originalNews.journalist(),
                    originalNews.originalNewsUrl(),
                    result.category()
            );

            return AnalyzedNewsDto.of(updatedNews, result.qualityScore(), result.category());
        }).toList();
    }

    // AI 응답 파싱용 내부 클래스
    private record NewsAnalyzedRes(
            @JsonProperty("newsIndex") int newsIndex,
            @JsonProperty("qualityScore") int qualityScore,
            @JsonProperty("category") NewsCategory category,
            @JsonProperty("cleanedContent") String cleanedContent
    ) {}
}