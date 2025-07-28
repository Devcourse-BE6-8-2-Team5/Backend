package com.back.domain.member.quizhistory.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.quizhistory.dto.QuizHistoryDto;
import com.back.domain.member.quizhistory.entity.QuizHistory;
import com.back.domain.member.quizhistory.repository.QuizHistoryRepository;
import com.back.domain.quiz.QuizType;
import com.back.domain.quiz.detail.repository.DetailQuizRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizHistoryService {

    private final QuizHistoryRepository quizHistoryRepository;

    // detail, fact, daily 퀴즈 도메인별
    private final DetailQuizRepository detailQuizRepository;
    //private final FactQuizRepository factQuizRepository;
    //private final DailyQuizRepository dailyQuizRepository;


    public QuizHistory createQuizHistory(Member actor, Long quizId, QuizType quizType, String answer) {

        // 퀴즈 정답 가져오기
        String correctAnswer =
                switch (quizType) {
                    case DETAIL -> detailQuizRepository.findById(quizId)
                            .orElseThrow(() -> new ServiceException(400, "존재하지 않는 퀴즈입니다."))
                            .getCorrectAnswerText();
                    //case FACT -> factQuizRepository.findById(quizId).orElseThrow(() -> new ServiceException(400,"존재하지 않는 퀴즈입니다.")).getCorrectAnswerText();
                    //case DAILY -> dailyQuizRepository.findById(quizId).orElseThrow(() -> new ServiceException(400,"존재하지 않는 퀴즈입니다.")).getCorrectAnswerText();

                    default -> throw new ServiceException(400, "지원하지 않는 퀴즈 타입입니다.");
                };


        // equals로 하면 안될거같고, contains로 해야할듯
        boolean isCorrect = correctAnswer.contains(answer);
        int gainExp = isCorrect ? 100 : 0; // 정답일 경우 경험치 100점 부여

        //경험치 적용
        actor.setExp(actor.getExp() + gainExp);

        // 퀴즈 히스토리 생성
        QuizHistory quizHistory = QuizHistory.builder()
                .quizId(quizId)
                .quizType(quizType)
                .answer(answer)
                .isCorrect(isCorrect)
                .gainExp(gainExp)
                .member(actor)
                .build();

        quizHistoryRepository.save(quizHistory);

        return quizHistory;
    }

    public List<QuizHistoryDto> getQuizHistoriesByMember(Member actor) {
        List<QuizHistory> quizHistories = quizHistoryRepository.findAllByMemberOrderByCreatedDateDesc(actor); //푼 시간을 기준으로 내림차순 정렬(최신 풀이부터)

        return quizHistories.stream()
                .map(QuizHistoryDto::new)
                .toList();
    }
}
