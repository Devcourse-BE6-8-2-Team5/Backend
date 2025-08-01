package com.back.domain.quiz.fact.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.quizhistory.entity.QuizHistory;
import com.back.domain.member.quizhistory.repository.QuizHistoryRepository;
import com.back.domain.member.quizhistory.service.QuizHistoryService;
import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.fake.entity.FakeNews;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.domain.quiz.QuizType;
import com.back.domain.quiz.fact.dto.FactQuizAnswerDto;
import com.back.domain.quiz.fact.dto.FactQuizDtoWithNewsContent;
import com.back.domain.quiz.fact.dto.FactQuizWithHistoryDto;
import com.back.domain.quiz.fact.entity.CorrectNewsType;
import com.back.domain.quiz.fact.entity.FactQuiz;
import com.back.domain.quiz.fact.repository.FactQuizRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactQuizService {
    private final FactQuizRepository factQuizRepository;
    private final RealNewsRepository realNewsRepository;
    private final MemberRepository memberRepository;
    private final QuizHistoryService quizHistoryService;
    private final QuizHistoryRepository quizHistoryRepository;

    @Transactional(readOnly = true)
    public List<FactQuiz> findAll() {
        return factQuizRepository.findAllWithNews();
    }

    @Transactional(readOnly = true)
    public List<FactQuiz> findByCategory(NewsCategory category) {
        return factQuizRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public FactQuizWithHistoryDto findById(Long id, Member actor) {
        FactQuiz factQuiz =  factQuizRepository.findByIdWithNews(id)
                .orElseThrow(() -> new ServiceException(404, "팩트 퀴즈를 찾을 수 없습니다. ID: " + id));

        FactQuizDtoWithNewsContent factQuizDto = new FactQuizDtoWithNewsContent(factQuiz);

        List<QuizHistory> histories = quizHistoryRepository.findByMember(actor);

        // 필터링: 퀴즈 ID가 일치하고, 퀴즈 타입이 FACT인 히스토리만 추출
        QuizHistory quizHistory = histories.stream()
                .filter(h -> h.getQuizId().equals(id) && h.getQuizType() == QuizType.FACT)
                .findFirst()
                .orElse(null);

        // 퀴즈 히스토리가 없으면 null 반환
        if (quizHistory == null) {
            return new FactQuizWithHistoryDto(
                    factQuizDto,
                    null,
                    false,
                    0
            );
        }

        return new FactQuizWithHistoryDto(
                factQuizDto,
                quizHistory.getAnswer(),
                quizHistory.isCorrect(),
                quizHistory.getGainExp()
        );

    }

    @Transactional
    public void create(RealNews realNews, FakeNews fakeNews) {
        FactQuiz quiz = createQuiz(realNews, fakeNews);
        //real.getFactQuizzes().add(quiz);
        //fake.getFactQuizzes().add(quiz);
        factQuizRepository.save(quiz);

        log.debug("팩트 퀴즈 생성 완료. 퀴즈 ID: {}, 뉴스 ID: {}", quiz.getId(), realNews.getId());
    }

    @Transactional
    public void delete(Long id) {
        FactQuiz quiz = factQuizRepository.findById(id)
                .orElseThrow(() -> new ServiceException(404, "팩트 퀴즈를 찾을 수 없습니다. ID: " + id));

        factQuizRepository.delete(quiz);
    }

    public long count() {
        return factQuizRepository.count();
    }

    // initData 전용
    @Transactional
    public void create(Long realNewsId) {
        RealNews real = realNewsRepository.findById(realNewsId)
                .orElseThrow(() -> new ServiceException(404, "진짜 뉴스를 찾을 수 없습니다. ID: " + realNewsId));

        FakeNews fake = real.getFakeNews();

        FactQuiz quiz = createQuiz(real, fake);

        //real.getFactQuizzes().add(quiz);
        //fake.getFactQuizzes().add(quiz);
        factQuizRepository.save(quiz);

        log.debug("팩트 퀴즈 생성 완료. 퀴즈 ID: {}, 뉴스 ID: {}", quiz.getId(), real.getId());
    }

    private FactQuiz createQuiz(RealNews real, FakeNews fake){
        // 퀴즈 질문과 정답은 랜덤으로 생성
        CorrectNewsType answerType = ThreadLocalRandom.current().nextBoolean()
                ? CorrectNewsType.REAL
                : CorrectNewsType.FAKE;

        String question = answerType == CorrectNewsType.REAL
                ? "다음 중 진짜 뉴스는?"
                : "다음 중 가짜 뉴스는?";

        return new FactQuiz(question, real, fake, answerType);
    }

    @Transactional
    public FactQuizAnswerDto submitDetailQuizAnswer(Member actor, Long id,CorrectNewsType selectedNewsType) {

        FactQuiz factQuiz = factQuizRepository.findById(id)
                .orElseThrow(() -> new ServiceException(404, "팩트 퀴즈를 찾을 수 없습니다"));

        Member managedActor = memberRepository.findById(actor.getId())
                .orElseThrow(() -> new ServiceException(404, "회원이 존재하지 않습니다."));

        boolean isCorrect = factQuiz.getCorrectNewsType() == selectedNewsType;
        int gainExp = isCorrect ? 10 : 0;

        managedActor.setExp(managedActor.getExp() + gainExp);

        quizHistoryService.save(
                managedActor,
                id,
                factQuiz.getQuizType(),
                String.valueOf(selectedNewsType),
                isCorrect,
                gainExp
        );

        return new FactQuizAnswerDto(
                factQuiz.getId(),
                factQuiz.getQuestion(),
                selectedNewsType,
                factQuiz.getCorrectNewsType(),
                isCorrect,
                gainExp,
                factQuiz.getQuizType()
        );

    }
}
