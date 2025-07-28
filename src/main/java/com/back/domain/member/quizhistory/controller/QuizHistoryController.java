package com.back.domain.member.quizhistory.controller;


import com.back.domain.member.member.entity.Member;
import com.back.domain.member.quizhistory.dto.QuizHistoryDto;
import com.back.domain.member.quizhistory.entity.QuizHistory;
import com.back.domain.member.quizhistory.service.QuizHistoryService;
import com.back.domain.quiz.QuizType;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/histories")
@RequiredArgsConstructor
@Tag(name = "QuizHistoryController", description = "퀴즈 히스토리 API 컨트롤러")
public class QuizHistoryController {

    private final QuizHistoryService quizHistoryService;
    private final Rq rq;

    record QuizHistoryCreateReqBody(
            @NotNull
            Long quizId,
            @NotNull
            QuizType quizType,
            @NotNull
            String answer
    ) {}

    @PostMapping
    @Operation(summary = "퀴즈 히스토리 생성 , 유저가 퀴즈를 풀고-> 서버에서 정답확인 + 경험치부여 -> 퀴즈 히스토리 생성 -> 클라이언트에 반환")
    @Transactional
    public RsData<QuizHistoryDto> createQuizHistory(@RequestBody @Valid QuizHistoryCreateReqBody reqBody){

        Member actor = rq.getActor();

        if (actor == null) {
            throw new ServiceException(401, "로그인이 필요합니다.");
        }

        // 퀴즈 히스토리 생성
        QuizHistory history = quizHistoryService.createQuizHistory(
                actor,
                reqBody.quizId,
                reqBody.quizType,
                reqBody.answer
        );

        return new RsData<>(
                200,
                "퀴즈 히스토리 생성 성공",
                new QuizHistoryDto(history)
        );
    }

}
