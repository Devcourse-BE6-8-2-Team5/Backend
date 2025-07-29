package com.back.domain.news.fake.entity;

import com.back.domain.news.real.entity.RealNews;
import com.back.domain.quiz.fact.entity.FactQuiz;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class FakeNews {
    @Id
    @OneToOne
    @MapsId // 진짜뉴스의 ID를 이 엔티티의 PK로 사용
    @JoinColumn(name = "real_news_id")
    private RealNews realNews;

    private String title;

    @Lob
    private String content;

    @OneToMany(mappedBy = "fakeNews", cascade = ALL, orphanRemoval = true)
    private List<FactQuiz> factQuizzes = new ArrayList<>();
}
