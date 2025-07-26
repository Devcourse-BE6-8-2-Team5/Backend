package com.back.domain.quiz.detail.controller;

import com.back.domain.quiz.detail.dto.DetailQuizDto;
import com.back.domain.quiz.detail.entity.DetailQuiz;
import com.back.domain.quiz.detail.service.DetailQuizService;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz/detail")
public class DetailQuizController {
    private final DetailQuizService detailQuizService;

    // 상세 퀴즈 목록 조회(전체 조회)
    @GetMapping
    public RsData<List<DetailQuizDto>> getDetailQuizzes() {
        List<DetailQuiz> detailQuizzes = detailQuizService.findAll();

        return new RsData<> (
                200,
                "상세 퀴즈 목록 조회 성공",
                detailQuizzes.stream()
                        .map(DetailQuizDto::new)
                        .toList()
        );
    }

    // 상세 퀴즈 단건 조회(퀴즈 ID로 조회)
    @GetMapping("/{id}")
    public RsData<DetailQuizDto> getDetailQuiz(@PathVariable Long id) {
        DetailQuiz detailQuiz = detailQuizService.findById(id);

        return new RsData<>(
                200,
                "상세 퀴즈 조회 성공",
                new DetailQuizDto(detailQuiz)
        );
    }

    // 상세 퀴즈 다건 조회(뉴스 ID로 조회)
    @GetMapping("/news/{newsId}")
    public RsData<List<DetailQuizDto>> getDetailQuizzesByNewsId(@PathVariable Long newsId) {
        List<DetailQuiz> detailQuizzes = detailQuizService.findByNewsId(newsId);

        return new RsData<>(
                200,
                "뉴스 ID로 상세 퀴즈 목록 조회 성공",
                detailQuizzes.stream()
                        .map(DetailQuizDto::new)
                        .toList()
        );
    }


    // 상세 퀴즈 생성(뉴스 ID로 찾은 뉴스의 퀴즈 모두 삭제 후 새로 생성해서 저장)
    @PostMapping("news/{newsId}/regenerate")
    public RsData<List<DetailQuizDto>> generateDetailQuizzes(@PathVariable Long newsId) {
        List<DetailQuizDto> newQuizzes = detailQuizService.generateQuizzes(newsId);
        List<DetailQuiz> savedQuizzes = detailQuizService.saveQuizzes(newsId, newQuizzes);

        return new RsData<>(
                201,
                "상세 퀴즈 생성 성공",
                savedQuizzes.stream()
                        .map(DetailQuizDto::new)
                        .toList()
        );
    }

}
