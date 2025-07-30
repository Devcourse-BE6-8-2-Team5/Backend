package com.back.domain.quiz.detail.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.quizhistory.service.QuizHistoryService;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.domain.quiz.detail.dto.DetailQuizAnswerDto;
import com.back.domain.quiz.detail.dto.DetailQuizDto;
import com.back.domain.quiz.detail.dto.DetailQuizReqDto;
import com.back.domain.quiz.detail.entity.DetailQuiz;
import com.back.domain.quiz.detail.entity.Option;
import com.back.domain.quiz.detail.repository.DetailQuizRepository;
import com.back.global.ai.AiService;
import com.back.global.ai.processor.DetailQuizProcessor;
import com.back.global.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j // 로그 확인용
public class DetailQuizService {
    private final DetailQuizRepository detailQuizRepository;
    private final RealNewsRepository realNewsRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final QuizHistoryService quizHistoryService;
    private final MemberRepository memberRepository;

    public long count() {
        return detailQuizRepository.count();
    }

    @Transactional(readOnly = true)
    public List<DetailQuiz> findAll() {
        return detailQuizRepository.findAll();
    }

    @Transactional(readOnly = true)
    public DetailQuiz findById(Long id) {
        return detailQuizRepository.findById(id)
                .orElseThrow(() -> new ServiceException(404, "해당 id의 상세 퀴즈가 존재하지 않습니다. id: " + id));
    }

    @Transactional(readOnly = true)
    public List<DetailQuiz> findByNewsId(Long newsId) {
        RealNews news = realNewsRepository.findById(newsId)
                .orElseThrow(() -> new ServiceException(404, "해당 id의 뉴스가 존재하지 않습니다. id: " + newsId));

        List<DetailQuiz> quizzes = detailQuizRepository.findByRealNewsId(newsId);

        if (quizzes.isEmpty()) {
            throw new ServiceException(404, "해당 뉴스에 대한 상세 퀴즈가 존재하지 않습니다. newsId: " + newsId);
        }

        return quizzes;
    }


    // newsId로 뉴스 조회 후 AI api 호출해 퀴즈 생성
    public List<DetailQuizDto> generateQuizzes(Long newsId) {
        RealNews news = realNewsRepository.findById(newsId)
                .orElseThrow(() -> new ServiceException(404, "해당 id의 뉴스가 존재하지 않습니다. id: " + newsId));

        DetailQuizReqDto req = new DetailQuizReqDto(
                news.getTitle(),
                news.getContent()
        );

        DetailQuizProcessor processor = new DetailQuizProcessor(req, objectMapper);

        return aiService.process(processor);
    }

    // 생성한 퀴즈 DB에 저장
    @Transactional
    public List<DetailQuiz> saveQuizzes(Long newsId, List<DetailQuizDto> quizzes) {
        RealNews news = realNewsRepository.findById(newsId)
                .orElseThrow(() -> new ServiceException(404, "해당 id의 뉴스가 존재하지 않습니다. id: " + newsId));

        detailQuizRepository.deleteByRealNewsId(newsId); // 기존 퀴즈 삭제

        List<DetailQuiz> savedQuizzes = quizzes.stream()
                .map(dto -> {
                    DetailQuiz quiz = new DetailQuiz(dto);
                    quiz.setRealNews(news); // RealNews 엔티티와 연관관계 설정
                    return quiz;
                })
                .toList();

        news.getDetailQuizzes().addAll(savedQuizzes);
        realNewsRepository.save(news); // RealNews 엔티티 저장 (CascadeType.ALL로 인해 DetailQuiz도 함께 저장됨)

        return savedQuizzes;
    }


    @Transactional
    public DetailQuiz updateDetailQuiz(Long id, DetailQuizDto detailQuizDto) {
        DetailQuiz quiz = detailQuizRepository.findById(id)
                .orElseThrow(() -> new ServiceException(404, "해당 id의 상세 퀴즈가 존재하지 않습니다. id: " + id));

        quiz.setQuestion(detailQuizDto.question());
        quiz.setOption1(detailQuizDto.option1());
        quiz.setOption2(detailQuizDto.option2());
        quiz.setOption3(detailQuizDto.option3());
        quiz.setCorrectOption(detailQuizDto.correctOption());

        return detailQuizRepository.save(quiz);
    }

    @Transactional
    public DetailQuizAnswerDto submitDetailQuizAnswer(Member actor, Long id, Option selectedOption) {

        DetailQuiz quiz = detailQuizRepository.findById(id)
                .orElseThrow(() -> new ServiceException(404, "해당 id의 상세 퀴즈가 존재하지 않습니다. id: " + id));

        boolean isCorrect = quiz.isCorrect(selectedOption);

        int gainExp = isCorrect ? 10 : 0; // 정답 제출 시 경험치 10점 부여

        // 영속 상태로 변경
        Member managedActor = memberRepository.findById(actor.getId())
                .orElseThrow(() -> new ServiceException(404, "회원이 존재하지 않습니다."));

        managedActor.setExp(managedActor.getExp() + gainExp);

        quizHistoryService.save(actor, id, quiz.getQuizType(), String.valueOf(selectedOption), isCorrect, gainExp); // 퀴즈 히스토리 저장

        return new DetailQuizAnswerDto(
                quiz.getId(),
                quiz.getQuestion(),
                quiz.getCorrectOption(),
                selectedOption,
                isCorrect,
                gainExp,
                quiz.getQuizType()
        );
    }
}
