package com.back.global.ai.processor;

import com.back.domain.quiz.detail.dto.DetailQuizDto;
import com.back.domain.quiz.detail.dto.DetailQuizCreateReqDto;
import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.List;

/**
 * 뉴스 제목과 본문을 기반 상세 퀴즈 3개를 생성하는 AI 요청 Processor 입니다.
 */
public class DetailQuizProcessor implements AiRequestProcessor<List<DetailQuizDto>> {
    private final DetailQuizCreateReqDto req;
    private final ObjectMapper objectMapper;

    public DetailQuizProcessor(DetailQuizCreateReqDto req, ObjectMapper objectMapper) {
        this.req = req;
        this.objectMapper = objectMapper;
    }

    // 뉴스 제목과 본문을 바탕으로 퀴즈 생성용 프롬프트 생성 (응답 형식을 JSON 형식으로 작성)
    @Override
    public String buildPrompt() {
        return String.format("""
                Task: 뉴스 제목과 본문을 바탕으로 퀴즈 3개를 생성하세요.
                
                목적:
                - 각 퀴즈는 사용자의 뉴스 내용 이해도를 평가하는 상세 퀴즈입니다.
                
                반드시 아래 조건을 지켜주세요:
                
                [퀴즈 형식]
                - 각 퀴즈는 다음 항목으로 구성됩니다:
                - question: 문제 내용
                - option1: 선택지 1
                - option2: 선택지 2
                - option3: 선택지 3
                - correctOption: 정답. 반드시 "OPTION1", "OPTION2", "OPTION3" 중 하나의 대문자 문자열로 표기하세요.
                
                [출제 기준]
                - 문제는 뉴스의 사실적 내용에 기반한 이해력 평가형이어야 합니다. 단순 상식이나 추론보다는 본문에서 명확히 언급된 정보를 묻는 형태로 만들어주세요.
                - 각 문제는 뉴스의 지엽적인 세부사항이 아니라, 뉴스의 중심 주제와 핵심 내용을 기반으로 출제되어야 합니다. 즉, 독자가 뉴스를 전반적으로 이해했는지를 평가할 수 있어야 합니다.
                - 오답(선택지)은 본문 내용과 유사하거나 혼동될 수 있지만, 실제로는 다른 내용을 담아야 합니다.
                
                [정답 조건 - 중요]
                - 정답(correctOption)은 퀴즈 3개에서 서로 달라야 합니다. 즉, "OPTION1", "OPTION2", "OPTION3"이 모두 최소 한 번씩 등장해야 합니다. 등장 순서는 랜덤으로 설정합니다.
                - 이 조건을 반드시 지키세요. 조건 위반 시 잘못된 출력으로 간주됩니다.
                
                응답 형식:
                응답은 반드시 아래 필드들을 포함한 JSON 배열 형식으로만 작성하세요. 설명 없이 JSON만 응답하세요.
                ```json
                [
                  {
                    "question": "문제 내용",
                    "option1": "선택지1",
                    "option2": "선택지2",
                    "option3": "선택지3",
                    "correctOption": "OPTION1" | "OPTION2" | "OPTION3"
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
    public List<DetailQuizDto> parseResponse(ChatResponse response) {

        String text = response.getResult().getOutput().getText();
        if (text == null || text.trim().isEmpty()) {
            throw new ServiceException(500, "AI 응답이 비어있습니다");
        }

        List<DetailQuizDto> result;

        // JSON 형식의 응답에서 ```json ... ``` 부분을 제거하여 순수 JSON 문자열로 변환
        String cleanedJson = text.replaceAll("(?s)```json\\s*(.*?)\\s*```", "$1").trim();

        try {
            // JSON 문자열을 DetailQuizResDto 객체 리스트로 변환
            // JSON의 키 이름과 변환하려는 객체(DetailQuizResDto)의 필드 이름이 일치해야 합니다.
            result = objectMapper.readValue(
                    cleanedJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DetailQuizDto.class)
            );
        } catch (Exception e) {
            throw new ServiceException(500, "AI 응답이 JSON 형식이 아닙니다. 응답: " + text);
        }

        if (result.size() != 3) {
            throw new ServiceException(500, "뉴스 하나당 3개의 퀴즈가 생성되어야 합니다. 생성된 수: " + result.size());
        }

        return result;

    }
}
