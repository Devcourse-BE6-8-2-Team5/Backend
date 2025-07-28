package com.back.domain.news.common.entity;

import com.back.domain.news.common.dto.KeywordWithType;
import com.back.domain.news.common.enums.KeywordType;
import com.back.domain.news.util.NewsCategory;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.*;

@Getter
@Entity
@NoArgsConstructor
public class KeywordHistory {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    // 키워드랑 타입 한번에 관리
    @Embedded
    private KeywordWithType keywordWithType;

    @Enumerated(EnumType.STRING)
    private NewsCategory category;

    private LocalDate usedDate; // 키워드가 사용된 날짜

    private LocalDateTime createAt; // 키워드가 생성된 시간


    @Builder
    public KeywordHistory(
            KeywordWithType keywordWithType,
            NewsCategory category,
            LocalDate usedDate
            ){
        this.keywordWithType = keywordWithType;
        this.category = category;
        this.usedDate = usedDate;
        this.createAt = LocalDateTime.now();
    }

}
