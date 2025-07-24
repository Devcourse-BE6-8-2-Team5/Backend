package com.back.domain.quiz.detail.entity;

import com.back.domain.news.realNews.entity.RealNews;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class DetailQuiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String question;

    private String option1;
    private String option2;
    private String option3;

    @Enumerated(EnumType.STRING)
    private Option correctOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "real_news_id")
    private RealNews realNews;

    // 정답 선택지 텍스트 반환
    public String getCorrectAnswerText() {
        return switch (correctOption) {
            case OPTION1 -> option1;
            case OPTION2 -> option2;
            case OPTION3 -> option3;
        };
    }

    public DetailQuiz(String question, String option1, String option2, String option3, Option correctOption) {
        this.question = question;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.correctOption = correctOption;
    }

}
