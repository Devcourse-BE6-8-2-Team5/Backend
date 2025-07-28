package com.back.domain.quiz.fact.repository;

import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.quiz.fact.entity.FactQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FactQuizRepository extends JpaRepository<FactQuiz, Long> {

    // FactQuiz에서 RealNews.newsCategory 기반으로 조회 (N+1 문제 방지를 위해 JOIN FETCH 사용)
    @Query("""
            SELECT fq
            FROM FactQuiz fq
            JOIN FETCH fq.realNews rn
            WHERE rn.newsCategory = :category
            """)
    List<FactQuiz> findByCategory(@Param("category") NewsCategory category);
}
