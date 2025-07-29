package com.back.domain.news.today.repository;

import com.back.domain.news.today.entity.TodayNews;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodayNewsRepository extends JpaRepository<TodayNews, Long> {
}
