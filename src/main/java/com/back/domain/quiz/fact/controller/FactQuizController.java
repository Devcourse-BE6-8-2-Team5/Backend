package com.back.domain.quiz.fact.controller;

import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.quiz.fact.dto.FactQuizDto;
import com.back.domain.quiz.fact.dto.FactQuizDtoWithNewsContent;
import com.back.domain.quiz.fact.entity.FactQuiz;
import com.back.domain.quiz.fact.service.FactQuizService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.stream.Collectors.toList;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz/fact")
@Tag(name = "FactQuizController", description = "팩트 퀴즈(진짜가짜 퀴즈) 관련 API")
public class FactQuizController {
    private final FactQuizService factQuizService;

    @Operation(summary = "팩트 퀴즈 전체 조회", description = "팩트 퀴즈 (전체) 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팩트 퀴즈 (전체) 목록 조회 성공")
    })
    @GetMapping
    public RsData<List<FactQuizDto>> getFactQuizzes() {
        List<FactQuiz> factQuizzes = factQuizService.findAll();

        return new RsData<>(
                200,
                "팩트 퀴즈 목록 조회 성공",
                factQuizzes.stream()
                        .map(FactQuizDto::new)
                        .collect(toList())
        );
    }

    @Operation(summary = "팩트 퀴즈 카테고리별 조회", description = "카테고리별로 팩트 퀴즈 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팩트 퀴즈 목록 조회 성공")
    })
    @GetMapping("/category")
    public RsData<List<FactQuizDto>> getFactQuizzesByCategory(@RequestParam NewsCategory category) {
        List<FactQuiz> factQuizzes = factQuizService.findByCategory(category);

        return new RsData<>(
                200,
                "팩트 퀴즈 목록 조회 성공. 카테고리: " + category,
                factQuizzes.stream()
                        .map(FactQuizDto::new)
                        .collect(toList())
        );
    }

    @Operation(summary = "팩트 퀴즈 단건 조회", description = "팩트 퀴즈 ID로 팩트 퀴즈를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팩트 퀴즈 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 ID의 팩트 퀴즈를 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RsData.class),
                            examples = @ExampleObject(value = "{\"resultCode\": 404, \"msg\": \"팩트 퀴즈를 찾을 수 없습니다. ID: 1\", \"data\": null}"))),
    })
    @GetMapping("/{id}")
    public RsData<FactQuizDtoWithNewsContent> getFactQuizById(@PathVariable Long id) {
        FactQuiz factQuiz = factQuizService.findById(id);

        return new RsData<>(
                200,
                "팩트 퀴즈 조회 성공. ID: " + id,
                new FactQuizDtoWithNewsContent(factQuiz)
        );
    }

    @Operation(summary = "팩트 퀴즈 삭제", description = "팩트 퀴즈 ID로 팩트 퀴즈를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팩트 퀴즈 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "팩트 퀴즈를 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RsData.class),
                            examples = @ExampleObject(value = "{\"resultCode\": 404, \"msg\": \"팩트 퀴즈를 찾을 수 없습니다. ID: 1\", \"data\": null}")))
    })
    @DeleteMapping("/{id}")
    public RsData<Void> deleteFactQuiz(@PathVariable Long id) {
        factQuizService.delete(id);
        return RsData.of(
                200,
                "팩트 퀴즈 삭제 성공. ID: " + id);
    }
}
