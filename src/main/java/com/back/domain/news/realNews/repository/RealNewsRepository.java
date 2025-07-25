package com.back.domain.news.realNews.repository;

import com.back.domain.news.realNews.entity.RealNews;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RealNewsRepository extends JpaRepository<RealNews, Long> {
    Page<RealNews> findByTitleContaining(String title, Pageable pageable);

    boolean existsByTitle(String title);
}
