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

    Page<RealNews> findByNewsCategory(NewsCategory category, Pageable pageable);

    Page<RealNews> findByTitleContainingAndIdNot(String title, Long excludedId, Pageable pageable);

    Page<RealNews> findByIdNot(Long excludedId, Pageable pageable);

    Page<RealNews> findByNewsCategoryAndIdNot(NewsCategory category, Long excludedId, Pageable pageable);


    boolean existsByLink(String url);

    List<RealNews> findByCreatedDateBetween(LocalDateTime start, LocalDateTime end);

    // 1. 제목 검색에서 카테고리별 N번째 제외 - 완전 최적화
    @Query(value = """
    SELECT /*+ USE_INDEX(rn, idx_real_news_title, idx_real_news_category_created_date) */ rn.*
    FROM real_news rn
    WHERE rn.title LIKE CONCAT('%', :title, '%')
      AND (:excludedId IS NULL OR rn.id != :excludedId)
      AND NOT EXISTS (
          SELECT /*+ USE_INDEX(sub, idx_real_news_title, idx_real_news_category_created_date) */ 1
          FROM (
              SELECT id,
                     ROW_NUMBER() OVER (PARTITION BY news_category ORDER BY created_date DESC) as category_rank
              FROM real_news /*+ USE_INDEX(idx_real_news_category_created_date) */
              WHERE title LIKE CONCAT('%', :title, '%')
                AND (:excludedId IS NULL OR id != :excludedId)
          ) ranked
          WHERE ranked.id = rn.id 
            AND ranked.category_rank = :excludedRank
      )
    ORDER BY rn.created_date DESC
    """,
            countQuery = """
    SELECT /*+ USE_INDEX(rn, idx_real_news_title) */ COUNT(*)
    FROM real_news rn
    WHERE rn.title LIKE CONCAT('%', :title, '%')
      AND (:excludedId IS NULL OR rn.id != :excludedId)
      AND NOT EXISTS (
          SELECT 1
          FROM (
              SELECT id,
                     ROW_NUMBER() OVER (PARTITION BY news_category ORDER BY created_date DESC) as category_rank
              FROM real_news /*+ USE_INDEX(idx_real_news_category_created_date) */
              WHERE title LIKE CONCAT('%', :title, '%')
                AND (:excludedId IS NULL OR id != :excludedId)
          ) ranked
          WHERE ranked.id = rn.id 
            AND ranked.category_rank = :excludedRank
      )
    """,
            nativeQuery = true)
    Page<RealNews> findByTitleExcludingNthCategoryRank(
            @Param("title") String title,
            @Param("excludedId") Long excludedId,
            @Param("excludedRank") int excludedRank,
            Pageable pageable);

    // 2. 전체 조회에서 카테고리별 N번째 제외 - 완전 최적화
    @Query(value = """
    SELECT /*+ USE_INDEX(rn, idx_real_news_created_date_desc) */ rn.*
    FROM real_news rn
    WHERE (:excludedId IS NULL OR rn.id != :excludedId)
      AND NOT EXISTS (
          SELECT /*+ USE_INDEX(sub, idx_real_news_category_created_date) */ 1 
          FROM (
              SELECT id, 
                     ROW_NUMBER() OVER (PARTITION BY news_category ORDER BY created_date DESC) as category_rank
              FROM real_news /*+ USE_INDEX(idx_real_news_category_created_date) */
              WHERE (:excludedId IS NULL OR id != :excludedId)
          ) ranked
          WHERE ranked.id = rn.id 
            AND ranked.category_rank = :excludedRank
      )
    ORDER BY rn.created_date DESC
    """,
            countQuery = """
    SELECT /*+ USE_INDEX(rn, idx_real_news_created_date_desc) */ COUNT(*)
    FROM real_news rn
    WHERE (:excludedId IS NULL OR rn.id != :excludedId)
      AND NOT EXISTS (
          SELECT 1 
          FROM (
              SELECT id, 
                     ROW_NUMBER() OVER (PARTITION BY news_category ORDER BY created_date DESC) as category_rank
              FROM real_news /*+ USE_INDEX(idx_real_news_category_created_date) */
              WHERE (:excludedId IS NULL OR id != :excludedId)
          ) ranked
          WHERE ranked.id = rn.id 
            AND ranked.category_rank = :excludedRank
      )
    """,
            nativeQuery = true)
    Page<RealNews> findAllExcludingNth(
            @Param("excludedId") Long excludedId,
            @Param("excludedRank") int excludedRank,
            Pageable pageable);

    // 3. 카테고리별 조회에서 N번째 제외 - 완전 최적화 (WITH절 사용)
    @Query(value = """
    WITH ranked_news AS (
        SELECT /*+ USE_INDEX(idx_real_news_category_created_date) */ 
               id, 
               ROW_NUMBER() OVER (ORDER BY created_date DESC) as rank_num
        FROM real_news
        WHERE news_category = :#{#category.name()}
          AND (:excludedId IS NULL OR id != :excludedId)
    )
    SELECT /*+ USE_INDEX(rn, idx_real_news_category_created_date) */ rn.*
    FROM real_news rn
    WHERE rn.news_category = :#{#category.name()}
      AND (:excludedId IS NULL OR rn.id != :excludedId)
      AND rn.id != COALESCE((
          SELECT r.id 
          FROM ranked_news r
          WHERE r.rank_num = :excludedRank
      ), 0)
    ORDER BY rn.created_date DESC
    """,
            countQuery = """
    WITH ranked_news AS (
        SELECT id, 
               ROW_NUMBER() OVER (ORDER BY created_date DESC) as rank_num
        FROM real_news /*+ USE_INDEX(idx_real_news_category_created_date) */
        WHERE news_category = :#{#category.name()}
          AND (:excludedId IS NULL OR id != :excludedId)
    )
    SELECT COUNT(*)
    FROM real_news rn
    WHERE rn.news_category = :#{#category.name()}
      AND (:excludedId IS NULL OR rn.id != :excludedId)
      AND rn.id != COALESCE((
          SELECT r.id 
          FROM ranked_news r
          WHERE r.rank_num = :excludedRank
      ), 0)
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
        WHERE news_category = :#{#category.name()}
    ) AS sub
    WHERE rn = :targetRank
    """,
            nativeQuery = true)
    Optional<RealNews> findNthRankByCategory(
            @Param("category") NewsCategory category,
            @Param("targetRank") int targetRank
    );

    // ===== 🚀 새로 추가된 최적화 메서드들 =====

    // 기본 전체 조회 (인덱스: idx_real_news_created_date_desc 직접 활용)
    Page<RealNews> findAllByOrderByCreatedDateDesc(Pageable pageable);

    // 카테고리별 조회 (인덱스: idx_real_news_category_created_date 직접 활용)
    Page<RealNews> findByNewsCategoryOrderByCreatedDateDesc(NewsCategory category, Pageable pageable);
    // ID 제외 조회들 (정렬 포함)
    Page<RealNews> findByIdNotOrderByCreatedDateDesc(Long excludedId, Pageable pageable);
    Page<RealNews> findByNewsCategoryAndIdNotOrderByCreatedDateDesc(NewsCategory category, Long excludedId, Pageable pageable);
    Page<RealNews> findByTitleContainingAndIdNotOrderByCreatedDateDesc(String title, Long excludedId, Pageable pageable);

    // 제목 검색 - 대소문자 무시 + 정렬 (인덱스: idx_real_news_title 활용)
    @Query("SELECT rn FROM RealNews rn WHERE LOWER(rn.title) LIKE LOWER(CONCAT('%', :title, '%')) ORDER BY rn.createdDate DESC")
    Page<RealNews> findByTitleContainingIgnoreCaseOrderByCreatedDateDesc(@Param("title") String title, Pageable pageable);


    // 관리자용 원본 날짜순 조회 (인덱스: idx_real_news_origin_created_date_desc)
    @Query("SELECT rn FROM RealNews rn ORDER BY rn.originCreatedDate DESC")
    Page<RealNews> findAllByOriginCreatedDateDesc(Pageable pageable);

    // 오늘의 뉴스 조회 (캐시 가능)
    @Query("SELECT rn FROM RealNews rn WHERE DATE(rn.createdDate) = CURRENT_DATE ORDER BY rn.createdDate DESC")
    Optional<RealNews> findTodayNews();

    // ===== 필수 유틸리티 메서드들 =====

    boolean existsByTitle(String title);

    // 날짜 범위 조회 - 정렬 추가로 인덱스 활용 개선
    @Query("SELECT rn FROM RealNews rn WHERE rn.createdDate BETWEEN :start AND :end ORDER BY rn.createdDate DESC")
    List<RealNews> findByCreatedDateBetweenOrderByCreatedDateDesc(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}

