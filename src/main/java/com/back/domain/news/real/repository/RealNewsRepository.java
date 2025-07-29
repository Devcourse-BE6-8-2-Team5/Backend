package com.back.domain.news.real.repository;

import com.back.domain.news.real.entity.RealNews;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RealNewsRepository extends JpaRepository<RealNews, Long> {
    Page<RealNews> findByTitleContaining(String title, Pageable pageable);

    boolean existsByTitle(String title);

    // 뉴스 가져오는 시간에 따라 쿼리 변경 가능성 있음
    @Query("SELECT r FROM RealNews r WHERE r.createdDate >= :start AND r.createdDate < :end")
    List<RealNews> findTodayNews(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

}
