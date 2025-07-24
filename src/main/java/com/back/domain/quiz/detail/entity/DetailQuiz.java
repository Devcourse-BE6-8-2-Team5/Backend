package com.back.domain.quiz.detail.entity;

import com.back.domain.news.realNews.entity.RealNews;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank(message = "Question can not be blank")
    private String question;

    @NotBlank(message = "Option1 can not be blank")
    private String option1;
    @NotBlank(message = "Option2 can not be blank")
    private String option2;
    @NotBlank(message = "Option3 can not be blank")
    private String option3;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Correct option must be specified")
    private Option correctOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "real_news_id")
    private RealNews realNews;

    // 정답 선택지 텍스트 반환
    public String getCorrectAnswerText() {
        if (correctOption == null) {
            return null; // global exception handler 추가되면 적절한 예외 던지게 수정
        }
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
