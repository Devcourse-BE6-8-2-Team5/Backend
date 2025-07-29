package com.back.domain.member.quizhistory.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.quizhistory.dto.QuizHistoryDto;
import com.back.domain.member.quizhistory.entity.QuizHistory;
import com.back.domain.member.quizhistory.repository.QuizHistoryRepository;
import com.back.domain.quiz.QuizType;
import com.back.domain.quiz.detail.repository.DetailQuizRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizHistoryService {

    private final QuizHistoryRepository quizHistoryRepository;
    private final MemberRepository memberRepository;

    // detail, fact, daily 퀴즈 도메인별
    private final DetailQuizRepository detailQuizRepository;
    //private final FactQuizRepository factQuizRepository;
    //private final DailyQuizRepository dailyQuizRepository;


    @Transactional
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

        // 퀴즈 히스토리 생성 , 연관관계 설정
        QuizHistory quizHistory = actor.addQuizHistory(quizId, quizType, answer, isCorrect, gainExp);


        // 양방향 연관관계 관리 및 저장, actor저장하면 quizHistory도 저장됨
        memberRepository.save(actor);

        return quizHistory;
    }

    @Transactional(readOnly = true)
    public List<QuizHistoryDto> getQuizHistoriesByMember(Member actor) {
        // Member의 quizHistories 리스트를 바로 가져오기 (Lazy라면 강제로 초기화 필요)
        List<QuizHistory> quizHistories = actor.getQuizHistories();

        // 필요하면 정렬 - createdDate 내림차순으로 정렬
        quizHistories.sort((a, b) -> b.getCreatedDate().compareTo(a.getCreatedDate()));

        return quizHistories.stream()
                .map(QuizHistoryDto::new)
                .toList();
    }
}
