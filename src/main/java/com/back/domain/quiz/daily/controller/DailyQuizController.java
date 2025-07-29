package com.back.domain.quiz.daily.controller;

import com.back.domain.quiz.daily.dto.DailyQuizDto;
import com.back.domain.quiz.daily.entity.DailyQuiz;
import com.back.domain.quiz.daily.service.DailyQuizService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quiz/daily")
@RequiredArgsConstructor
@Tag(name= "DailyQuizController", description = "오늘의 퀴즈 관련 API")
public class DailyQuizController {
    private final DailyQuizService dailyQuizService;

    @Operation(summary = "오늘의 퀴즈 조회", description = "오늘의 뉴스 ID로 오늘의 퀴즈(3개)를 조회합니다.")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "오늘의 퀴즈 조회 성공",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = RsData.class),
                                    examples = @ExampleObject(value = "{\"resultCode\": 200, \"msg\": \"오늘의 퀴즈 조회 성공\", \"data\": [{\"id\": 1, \"question\": \"문제1\", \"option1\": \"선택지1\", \"option2\": \"선택지2\", \"option3\": \"선택지3\", \"correctOption\": \"OPTION1\"}]}"))),
                    @ApiResponse(responseCode = "404", description = "오늘의 뉴스에 해당하는 오늘의 퀴즈가 존재하지 않음",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = RsData.class),
                                    examples = @ExampleObject(value = "{\"resultCode\": 404, \"msg\": \"오늘의 뉴스에 해당하는 오늘 퀴즈가 존재하지 않습니다.\", \"data\": null}")))
            }
    )
    @GetMapping("/{todayNewsId}")
    public RsData<List<DailyQuizDto>> getDailyQuizzes(@PathVariable Long todayNewsId) {
        List<DailyQuiz> dailyQuizzes = dailyQuizService.getDailyQuizzes(todayNewsId);
        return RsData.of(
                200,
                "오늘의 퀴즈 조회 성공",
                dailyQuizzes.stream()
                        .map(DailyQuizDto::new)
                        .toList()
        );
    }
}
