package com.back.domain.news.today.entity;

import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.quiz.detail.entity.DetailQuiz;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.*;

@Entity
@Getter
@NoArgsConstructor
public class TodayNews {
    @Id
    private Long id;

    private LocalDate selectedDate;

    @OneToOne
    @MapsId
    @JoinColumn(name = "real_news_id")
    private RealNews realNews;

    @Builder
    public TodayNews(LocalDate selectedDate, RealNews realNews) {
        this.selectedDate = selectedDate;
        this.realNews = realNews;
    }
}