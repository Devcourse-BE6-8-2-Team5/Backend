package com.back.global.ai.processor;

import com.back.domain.news.common.dto.KeywordGenerationReqDto;
import com.back.domain.news.common.dto.KeywordGenerationResDto;
import com.back.domain.news.fake.dto.FakeNewsDto;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.global.exception.ServiceException;
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
               Task: 제공된 진짜 뉴스를 바탕으로 가짜 뉴스를 생성하세요.
               
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
               
               ⚠️ 매우 중요: 반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트나 설명은 포함하지 마세요.
               
               ```json
               {
                 "realNewsId": %s,  // ← 따옴표 제거 (숫자로 처리)
                 "title": "생성된 가짜 뉴스 제목",
                 "content": "생성된 가짜 뉴스 본문 전체"
               }
               ```
               """,
                req.title(),
                req.content(),
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

        FakeNewsDto result;
        String cleanedJson = text.replaceAll("(?s)```json\\s*(.*?)\\s*```", "$1").trim();

        try {
            result = objectMapper.readValue(
                    cleanedJson,
                    FakeNewsDto.class
            );
        } catch (Exception e) {
            throw new ServiceException(500, "AI 응답이 JSON 형식이 아닙니다. 응답 : " + cleanedJson);
        }
        return result;
    }
}