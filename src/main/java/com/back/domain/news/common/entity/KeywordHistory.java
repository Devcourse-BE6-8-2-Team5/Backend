package com.back.domain.news.common.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.*;

@Entity
@NoArgsConstructor
public class KeywordHistory {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private String keyword; // 검색된 키워드

//    @Enumerated(EnumType.STRING)
//    private NewsCategory category;

    private LocalDate usedDate; // 키워드가 사용된 날짜

    private LocalDateTime createAt; // 키워드가 생성된 시간

    private Integer usageCount=1; // 키워드 사용 횟수

    private Integer collectedNewsCount =0 ; // 해당 키워드로 수집된 뉴스 개수

    private Double efficiencyScore; // 효율성 점수 (0.0 ~ 1.0)

    @Builder
    public KeywordHistory(
            String keyword,
            //NewsCategory category,
            LocalDate usedDate
            ){
        this.keyword = keyword;
//        this.category = category;
        this.usedDate = usedDate;
        this.createAt = LocalDateTime.now();
    }

}
