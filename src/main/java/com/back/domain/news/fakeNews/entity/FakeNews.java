package com.back.domain.news.fakeNews.entity;

import com.back.domain.news.realNews.entity.RealNews;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
public class FakeNews {
    @Id
    private Long id;

    @OneToOne
    @MapsId // 진짜뉴스의 ID를 이 엔티티의 PK로 사용
    @JoinColumn(name = "id")
    private RealNews realNews;

    private String title;

    @Lob
    private String content;
}
