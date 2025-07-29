package com.back.global.ai.processor;

import com.back.domain.news.common.dto.KeywordGenerationReqDto;
import com.back.domain.news.common.dto.KeywordGenerationResDto;
import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * 뉴스 제목과 본문을 기반 상세 퀴즈 3개를 생성하는 AI 요청 Processor 입니다.
 */
public class FakeNewsGeneratorProcessor implements AiRequestProcessor<KeywordGenerationResDto> {
    private final KeywordGenerationReqDto req;
    private final ObjectMapper objectMapper;

    public FakeNewsGeneratorProcessor(KeywordGenerationReqDto req, ObjectMapper objectMapper) {
        this.req = req;
        this.objectMapper = objectMapper;
    }

    // 뉴스 제목과 본문을 바탕으로 퀴즈 생성용 프롬프트 생성 (응답 형식을 JSON 형식으로 작성)
    @Override
    public String buildPrompt() {
        return String.format("""
                ~~
                ~~
                
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
        return result;

    }
}
