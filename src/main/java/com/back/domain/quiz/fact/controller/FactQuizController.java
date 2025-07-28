package com.back.domain.quiz.fact.controller;

import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.quiz.fact.dto.FactQuizDto;
import com.back.domain.quiz.fact.dto.FactQuizDtoWithNewsContent;
import com.back.domain.quiz.fact.entity.FactQuiz;
import com.back.domain.quiz.fact.service.FactQuizService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/quiz/fact")
public class FactQuizController {
    private final FactQuizService factQuizService;

    @Operation(summary = "팩트 퀴즈 전체 조회", description = "팩트 퀴즈 목록을 조회합니다.")
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
    @GetMapping("/category")
    public RsData<List<FactQuizDto>> getFactQuizzesByCategory(@RequestParam NewsCategory category) {
        List<FactQuiz> factQuizzes = factQuizService.findByCategory(category);

        return new RsData<>(
                200,
                "팩트 퀴즈 목록 조회 성공",
                factQuizzes.stream()
                        .map(FactQuizDto::new)
                        .collect(toList())
        );
    }

    @Operation(summary = "팩트 퀴즈 단건 조회", description = "팩트 퀴즈 ID로 팩트 퀴즈를 조회합니다.")
    @GetMapping("/{id}")
    public RsData<FactQuizDtoWithNewsContent> getFactQuizById(@PathVariable Long id) {
        FactQuiz factQuiz = factQuizService.findById(id);

        return new RsData<>(
                200,
                "팩트 퀴즈 조회 성공",
                new FactQuizDtoWithNewsContent(factQuiz)
        );
    }
}
