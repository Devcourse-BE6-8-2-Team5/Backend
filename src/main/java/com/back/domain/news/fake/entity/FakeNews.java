package com.back.domain.news.fake.entity;

import com.back.domain.news.real.entity.RealNews;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
public class FakeNews {
    @Id
    private Long id;

    @OneToOne
    @MapsId // 진짜뉴스의 ID를 이 엔티티의 PK로 사용
    @JoinColumn(name = "real_news_id")
    private RealNews realNews;

    private String title;

    @Lob
    private String content;
}
