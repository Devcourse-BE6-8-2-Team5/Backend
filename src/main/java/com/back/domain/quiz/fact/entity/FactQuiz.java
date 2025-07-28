package com.back.domain.quiz.fact.entity;

import com.back.domain.news.fake.entity.FakeNews;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.quiz.QuizType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Service
@NoArgsConstructor
public class FactQuiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Question can not be blank")
    private String question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "real_news_id", nullable = false)
    private RealNews realNews;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fake_news_id", nullable = false)
    private FakeNews fakeNews;

    @Enumerated(EnumType.STRING)
    private CorrectNewsType correctNewsType;

    @Enumerated(EnumType.STRING)
    private QuizType quizType = QuizType.FACT;

    @CreatedDate
    private LocalDateTime createdDate; // 생성 날짜(DB에 저장된 날짜)

}