package com.back.global.ai.processor;

import com.back.domain.quiz.detail.dto.DetailQuizReqDto;
import com.back.domain.quiz.detail.dto.DetailQuizResDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.List;

/**
 * 뉴스 제목과 본문을 기반 상세 퀴즈 3개를 생성하는 AI 요청 Processor 입니다.
 *
 */
public class DetailQuizProcessor implements AiRequestProcessor<List<DetailQuizResDto>> {
    private final DetailQuizReqDto req;
    private final ObjectMapper objectMapper;

    public DetailQuizProcessor(DetailQuizReqDto req, ObjectMapper objectMapper) {
        this.req = req;
        this.objectMapper = objectMapper;
    }

    // 뉴스 제목과 본문을 바탕으로 퀴즈 생성용 프롬프트 생성 (응답 형식을 JSON 형식으로 작성)
    @Override
    public String buildPrompt() {
        return String.format("""
                Task: 뉴스 제목과 본문을 바탕으로 퀴즈 3개를 생성하세요.
                
                - 각 퀴즈는 사용자의 뉴스 내용 이해도를 평가하는 상세 퀴즈입니다.
                - 퀴즈는 다음 기준을 반드시 따라야 합니다:
                  1. 문제(question), 선택지(option1, option2, option3), 정답(correctOption)으로 구성합니다.
                  2. 문제는 뉴스의 사실적 내용에 기반한 이해력 평가형이어야 하며, 단순 상식이나 추론보다는 본문에서 명확히 언급된 정보를 묻는 형태여야 합니다.
                  3. 오답(선택지)은 본문과 일부 유사하거나 혼동될 수 있지만 실제 내용과는 다른 정보를 포함해야 합니다.
                  4. 정답(correctOption)은 option1, option2, option3 중 하나이며, 반드시 본문에서 근거를 찾을 수 있어야 합니다.
                  5. **문제는 뉴스의 지엽적인 세부사항이 아니라, 중심 주제와 핵심 내용을 기반으로 출제되어야 합니다.** 
                       즉, 독자가 뉴스를 전반적으로 이해했는지를 평가할 수 있어야 합니다.
                
                응답은 반드시 아래 필드들을 포함한 JSON 형식으로만 작성하세요:
                ```json
                [
                  {
                    "question": "문제 내용",
                    "option1": "선택지1",
                    "option2": "선택지2",
                    "option3": "선택지3",
                    "correctOption": "정답 선택지(option1, option2, option3 중 하나)"
                  },
                  ...
                ]
                
                input:
                {
                    "title": "%s",
                    "content": "%s"
                }
                """, req.title(), req.content());
    }

    // AI 응답을 파싱하여 DetailQuizResDto 리스트로 변환
    @Override
    public List<DetailQuizResDto> parseResponse(ChatResponse response) {
        try {
            String text = response.getResult().getOutput().getText();
            // JSON 형식의 응답에서 ```json ... ``` 부분을 제거하여 순수 JSON 문자열로 변환
            String cleanedJson = text.replaceAll("(?s)```json\\s*(.*?)\\s*```", "$1").trim();

            // JSON 문자열을 DetailQuizResDto 객체 리스트로 변환
            // JSON의 키 이름과 변환하려는 객체(DetailQuizResDto)의 필드 이름이 일치해야 합니다.
            return objectMapper.readValue(
                    cleanedJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DetailQuizResDto.class)
            );
        } catch (Exception e) {
            throw new RuntimeException("AI 응답 파싱 실패: " + e.getMessage(), e);
        }
    }
}
