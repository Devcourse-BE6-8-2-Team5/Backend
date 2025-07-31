package com.back.domain.quiz.daily.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.quizhistory.entity.QuizHistory;
import com.back.domain.member.quizhistory.repository.QuizHistoryRepository;
import com.back.domain.member.quizhistory.service.QuizHistoryService;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.repository.TodayNewsRepository;
import com.back.domain.news.today.entity.TodayNews;
import com.back.domain.quiz.QuizType;
import com.back.domain.quiz.daily.dto.DailyQuizAnswerDto;
import com.back.domain.quiz.daily.dto.DailyQuizDto;
import com.back.domain.quiz.daily.dto.DailyQuizWithHistoryDto;
import com.back.domain.quiz.daily.entity.DailyQuiz;
import com.back.domain.quiz.daily.repository.DailyQuizRepository;
import com.back.domain.quiz.detail.entity.DetailQuiz;
import com.back.domain.quiz.detail.entity.Option;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyQuizService {
    private final DailyQuizRepository dailyQuizRepository;
    private final TodayNewsRepository todayNewsRepository;
    private final MemberRepository memberRepository;
    private final QuizHistoryService quizHistoryService;
    private final QuizHistoryRepository quizHistoryRepository;

    @Transactional(readOnly = true)
    public List<DailyQuizWithHistoryDto> getDailyQuizzes(Long todayNewsId, Member actor) {
        //퀴즈 가져오기
        List<DailyQuiz> quizzes = dailyQuizRepository.findByTodayNewsId(todayNewsId);

        if( quizzes.isEmpty()) {
            throw new ServiceException(404, "오늘의 뉴스에 해당하는 오늘의 퀴즈가 존재하지 않습니다.");
        }

        //이제 여기서 퀴즈 저장소에서 memberid로 퀴즈 히스토리를 가져와야 함
        List<QuizHistory> quizHistories = quizHistoryRepository.findByMember(actor);

        // 1. 기준이 되는 DailyQuiz 리스트에서 ID만 추출
        Set<Long> quizIdSet = quizzes.stream()
                .map(DailyQuiz::getId)
                .collect(Collectors.toSet());

        // 2. 퀴즈 히스토리 중에서 quizId가 일치하고, 타입이 DAILY인 것만 필터링
        List<QuizHistory> filteredHistories = quizHistories.stream()
                .filter(h -> h.getQuizType() == QuizType.DAILY && quizIdSet.contains(h.getQuizId()))
                .toList();

        Map<Long, QuizHistory> historyMap = filteredHistories.stream()
                .collect(Collectors.toMap(QuizHistory::getQuizId, h -> h));

        // 3. DailyQuizWithHistoryDto로 변환
        List<DailyQuizWithHistoryDto> result = quizzes.stream()
                .map(quiz -> {
                    QuizHistory history = historyMap.get(quiz.getId());
                    return new DailyQuizWithHistoryDto(
                            Collections.singletonList(new DailyQuizDto(quiz)),
                            history != null ? history.getAnswer() : null,
                            history != null && history.isCorrect(),
                            history != null ? history.getGainExp() : 0,
                            QuizType.DAILY
                    );
                })
                .toList();

        return result;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void createDailyQuiz() {
        TodayNews todayNews = todayNewsRepository.findFirstByOrderBySelectedDateDesc()
                .orElseThrow(() -> new ServiceException(404, "오늘의 뉴스가 없습니다."));

        boolean alreadyCreated = dailyQuizRepository.existsByTodayNews(todayNews);

        if (alreadyCreated) {
            log.info("이미 오늘의 퀴즈가 생성되었습니다. 작업을 건너뜁니다.");
            return;
        }

        RealNews realNews = todayNews.getRealNews();

        List<DetailQuiz> quizzes = realNews.getDetailQuizzes();

        if (quizzes == null || quizzes.isEmpty()) {
            throw new ServiceException(400, "연결된 상세 퀴즈가 없습니다.");
        }

        List<DailyQuiz> dailyQuizzes = quizzes.stream()
                .map(quiz -> new DailyQuiz(todayNews, quiz))
                .toList();

        dailyQuizRepository.saveAll(dailyQuizzes);

        log.info("오늘의 퀴즈 생성 완료");
    }

    public long count() {
        return dailyQuizRepository.count();
    }

    //InitData 전용
    @Transactional
    public void createDailyQuizForInitData() {
        TodayNews todayNews = todayNewsRepository.findAll().getFirst();

        RealNews realNews = todayNews.getRealNews();
        List<DetailQuiz> quizzes = realNews.getDetailQuizzes();

        List<DailyQuiz> dailyQuizzes = quizzes.stream()
                .map(quiz -> new DailyQuiz(todayNews, quiz))
                .toList();

        dailyQuizRepository.saveAll(dailyQuizzes);

    }

    @Transactional
    public DailyQuizAnswerDto submitDetailQuizAnswer(Member actor, Long id, Option selectedOption) {
        DailyQuiz dailyQuiz = dailyQuizRepository.findById(id)
                .orElseThrow(() -> new ServiceException(404, "오늘의 퀴즈를 찾을 수 없습니다."));

        Member managedActor = memberRepository.findById(actor.getId())
                .orElseThrow(() -> new ServiceException(404, "회원이 존재하지 않습니다."));

        DetailQuiz detailQuiz = dailyQuiz.getDetailQuiz();

        boolean isCorrect = detailQuiz.isCorrect(selectedOption);
        int gainExp = isCorrect ? 10 : 0;

        managedActor.setExp(managedActor.getExp() + gainExp);

        quizHistoryService.save(managedActor, id, dailyQuiz.getQuizType(), String.valueOf(selectedOption), isCorrect, gainExp); // 퀴즈 히스토리 저장

        return new DailyQuizAnswerDto(
                dailyQuiz.getId(),
                detailQuiz.getQuestion(),
                detailQuiz.getCorrectOption(),
                selectedOption,
                isCorrect,
                gainExp,
                dailyQuiz.getQuizType()
        );

    }
}
