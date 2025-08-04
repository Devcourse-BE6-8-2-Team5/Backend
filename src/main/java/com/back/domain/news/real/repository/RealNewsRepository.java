package com.back.domain.news.real.repository;

import com.back.domain.news.common.enums.NewsCategory;
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

    Page<RealNews> findByNewsCategory(NewsCategory category, Pageable pageable);

    Page<RealNews> findByTitleContainingAndIdNot(String title, Long excludedId, Pageable pageable);

    Page<RealNews> findByIdNot(Long excludedId, Pageable pageable);

    Page<RealNews> findByNewsCategoryAndIdNot(NewsCategory category, Long excludedId, Pageable pageable);


    boolean existsByLink(String url);

    List<RealNews> findByCreatedDateBetween(LocalDateTime start, LocalDateTime end);

    @Query(value = """
    SELECT * FROM (
        SELECT *,
            ROW_NUMBER() OVER (PARTITION BY news_category ORDER BY created_date DESC) AS rn
        FROM real_news
        WHERE title LIKE CONCAT('%', :title, '%')
          AND (:excludedId IS NULL OR id != :excludedId)
    ) AS sub
    WHERE rn != :excludedRank
    ORDER BY created_date DESC
    """,
            countQuery = """
    SELECT COUNT(*) FROM (
        SELECT ROW_NUMBER() OVER (PARTITION BY news_category ORDER BY created_date DESC) AS rn
        FROM real_news
        WHERE title LIKE CONCAT('%', :title, '%')
          AND (:excludedId IS NULL OR id != :excludedId)
    ) AS sub
    WHERE rn != :excludedRank
    """,
            nativeQuery = true)
    Page<RealNews> findByTitleExcludingNthCategoryRank(
            @Param("title") String title,
            @Param("excludedId") Long excludedId,
            @Param("excludedRank") int excludedRank,
            Pageable pageable);

    @Query(value = """
    SELECT *
    FROM (
        SELECT *,
               ROW_NUMBER() OVER (PARTITION BY news_category ORDER BY created_date DESC) AS rn
        FROM real_news
        WHERE (:excludedId IS NULL OR id != :excludedId)
    ) AS sub
    WHERE rn != :excludedRank
    ORDER BY created_date DESC
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM (
        SELECT ROW_NUMBER() OVER (PARTITION BY news_category ORDER BY created_date DESC) AS rn
        FROM real_news
        WHERE (:excludedId IS NULL OR id != :excludedId)
    ) AS sub
    WHERE rn != :excludedRank
    """,
            nativeQuery = true)
    Page<RealNews> findAllExcludingNth(
            @Param("excludedId") Long excludedId,
            @Param("excludedRank") int excludedRank,
            Pageable pageable);


    @Query(value = """
    SELECT *
    FROM (
        SELECT *,
               ROW_NUMBER() OVER (ORDER BY created_date DESC) AS rn
        FROM real_news
        WHERE news_category = :category
          AND (:excludedId IS NULL OR id != :excludedId)
    ) AS sub
    WHERE rn != :excludedRank
    ORDER BY created_date DESC
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM (
        SELECT ROW_NUMBER() OVER (ORDER BY created_date DESC) AS rn
        FROM real_news
        WHERE news_category = :category
          AND (:excludedId IS NULL OR id != :excludedId)
    ) AS sub
    WHERE rn != :excludedRank
    """,
            nativeQuery = true)
    Page<RealNews> findByCategoryExcludingNth(
            @Param("category") NewsCategory category,
            @Param("excludedId") Long excludedId,
            @Param("excludedRank") int excludedRank,
            Pageable pageable);

    @Query(value = """
    SELECT *
    FROM (
        SELECT *,
               ROW_NUMBER() OVER (PARTITION BY news_category ORDER BY created_date DESC) AS rn
        FROM real_news
    ) AS sub
    WHERE rn = :targetRank
    ORDER BY created_date DESC
    """,
            nativeQuery = true)
    List<RealNews> findNthRankByAllCategories(@Param("targetRank") int targetRank);

    // 특정 카테고리에서 N번째 순위 뉴스 조회
    @Query(value = """
    SELECT *
    FROM (
        SELECT *,
               ROW_NUMBER() OVER (ORDER BY created_date DESC) AS rn
        FROM real_news
        WHERE news_category = :category
    ) AS sub
    WHERE rn = :targetRank
    """,
            nativeQuery = true)
    Optional<RealNews> findNthRankByCategory(
            @Param("category") NewsCategory category,
            @Param("targetRank") int targetRank
    );
}

