package com.back.domain.news.realNews.entity;


import com.back.domain.quiz.detail.entity.DetailQuiz;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Getter
@NoArgsConstructor
public class RealNews {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private long id;

    private String title;

    @Lob
    private String content;

    private String link;
    private String imgUrl;
    private String description;
    private LocalDateTime originCreatedDate; // 원본 뉴스 생성 날짜

    private String originalNewsUrl;  // 원본 뉴스 url
    private String mediaName; // 뉴스 매체 이름 (예: BBC, CNN 등)
    private String journalist; // 뉴스 작성자 (기자)

    // 상세 퀴즈와 1:N 관계 설정 (RealNews 하나 당 3개의 DetailQuiz가 생성됩니다.)
    @OneToMany(mappedBy = "realNews", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetailQuiz> detailQuizzes = new ArrayList<>();

    // boolean is_fake    < 가짜 뉴스랑 1 대 1 매핑이면 필요없을 것 같음
    // 카테고리id나 가짜테이블 연관설정은 테이블 추가될때 같이 수정할 예정

    @Builder
    public RealNews(
            String title,
            String content,
            String description,
            String link,
            String imgUrl,
            LocalDateTime originCreatedDate,
            String mediaName,
            String journalist,
            String originalNewsUrl) {
        this.title = title;
        this.content = content;
        this.description = description;
        this.link = link;
        this.imgUrl = imgUrl;
        this.originCreatedDate = originCreatedDate;
        this.mediaName = mediaName;
        this.journalist = journalist;
        this.originalNewsUrl = originalNewsUrl;
    }

}
