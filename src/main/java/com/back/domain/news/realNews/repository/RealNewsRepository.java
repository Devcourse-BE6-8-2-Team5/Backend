package com.back.domain.news.realNews.repository;

import com.back.domain.news.realNews.entity.RealNews;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RealNewsRepository extends JpaRepository<RealNews, Long> {
}
