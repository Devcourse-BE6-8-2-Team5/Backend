package com.back.global.ai.processor;

import com.back.domain.news.common.dto.AnalyzedNewsDto;
import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.List;

@Slf4j
public class NewsAnalysisProcessor implements AiRequestProcessor<List<AnalyzedNewsDto>> {

    private final List<RealNewsDto> newsToAnalyze;
    private final ObjectMapper objectMapper;

    public NewsAnalysisProcessor(List<RealNewsDto> newsToAnalyze, ObjectMapper objectMapper) {
        this.newsToAnalyze = newsToAnalyze;
        this.objectMapper = objectMapper;
    }

    @Override
    public String buildPrompt() {
        StringBuilder newsInput = new StringBuilder();
        for (int i = 0; i < newsToAnalyze.size(); i++) {
            RealNewsDto news = newsToAnalyze.get(i);
            newsInput.append(String.format("""
                뉴스 %d:

                내용: %s
                ---
                """, i + 1,  cleanText(news.content())));
        }

        return String.format("""
               Task: 제공된 뉴스들을 분석하여 품질 점수와 카테고리를 분류하세요.
               
               목적:
               - 각 뉴스의 품질을 1-100점으로 평가
               - 뉴스를 5개 카테고리 중 하나로 분류
               - 사용자에게 가치 있는 뉴스를 선별하기 위함
               
               [품질 평가 기준]
               점수는 다음 기준으로 종합 평가하세요:
               1. 정보의 완성도와 구체성 (30점)
                  - 구체적인 사실, 수치, 날짜, 장소가 포함되어 있는가?
                  - 정보가 충분히 상세하고 이해하기 쉬운가?
               
               2. 뉴스 가치와 중요성 (25점)
                  - 사회적으로 의미 있는 내용인가?
                  - 많은 사람들에게 영향을 미치는 이슈인가?
                  - 시의성이 있는 내용인가?
               
               3. 내용의 신뢰성과 객관성 (25점)
                  - 사실에 기반한 내용인가?
                  - 균형 잡힌 시각으로 작성되었는가?
                  - 선정적이거나 과장되지 않았는가?
               
               4. 기사 품질과 완성도 (20점)
                  - 문장이 자연스럽고 이해하기 쉬운가?
                  - 논리적 구조를 가지고 있는가?
                  - 오탈자나 어색한 표현이 없는가?
               
               [카테고리 분류 기준]
               다음 5개 카테고리 중 가장 적합한 하나를 선택하세요:
               
               - POLITICS: 정치, 선거, 정부 정책, 외교, 국회, 지방자치 관련
               - ECONOMY: 경제, 금융, 기업, 산업, 무역, 주식, 부동산, 고용 관련
               - SOCIETY: 사회 이슈, 교육, 복지, 범죄, 사고, 환경, 인구, 지역 사회 관련
               - CULTURE: 문화, 예술, 스포츠, 연예, 관광, 여행, 축제, 종교 관련
               - IT: 과학기술, IT, 인공지능, 바이오, 우주, 통신, 게임, 디지털 관련
               
               [주의사항]
               - 점수는 반드시 1-100 사이의 정수로 제공
               - 카테고리는 반드시 위의 5개 중 하나만 선택 (대문자로 정확히 표기)
               - 객관적이고 공정한 평가를 수행
               - 개인적 편견이나 주관적 취향을 배제
               
               [출력 규칙]
               - 마크다운 코드 블록 사용 금지
               - 아래 JSON 형식으로만 응답
               - 다른 텍스트나 설명 포함 금지
               
               응답 형식:
               [
                 {
                   "newsIndex": 1,
                   "qualityScore": 85,
                   "category": "POLITICS"
                 },
                 {
                   "newsIndex": 2,
                   "qualityScore": 72,
                   "category": "ECONOMY"
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

        } catch (Exception e) {
            log.error("JSON 파싱 실패: {}", e.getMessage());
            throw new ServiceException(500, "AI 응답 파싱에 실패했습니다: " + e.getMessage());
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
                    originalNews.content(),
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
            int newsIndex,
            int qualityScore,
            NewsCategory category
    ) {}
}