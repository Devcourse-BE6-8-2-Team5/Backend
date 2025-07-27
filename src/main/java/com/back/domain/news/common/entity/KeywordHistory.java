package com.back.domain.news.common.entity;

import com.back.domain.news.common.enums.KeywordType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private String keyword; // 검색된 키워드

//    @Enumerated(EnumType.STRING)
//    private NewsCategory category;

    private LocalDate usedDate; // 키워드가 사용된 날짜

    private LocalDateTime createAt; // 키워드가 생성된 시간

    @Enumerated(EnumType.STRING)
    private KeywordType keywordType;

    @Builder
    public KeywordHistory(
            String keyword,
            //NewsCategory category,
            LocalDate usedDate,
            KeywordType keywordType
            ){
        this.keyword = keyword;
//        this.category = category;
        this.usedDate = usedDate;
        this.createAt = LocalDateTime.now();
        this.keywordType = keywordType;
    }

}
