package com.back.domain.news.common.repository;

import com.back.domain.news.common.entity.KeywordHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface KeywordHistoryRepository extends JpaRepository<KeywordHistory,Long> {

    /**
     * 최근 N일간 M회 이상 사용된 키워드 조회 (과도 사용 키워드)
     */
    @Query("""
        SELECT kh.keywordWithType.keyword 
        FROM KeywordHistory kh 
        WHERE kh.usedDate >= :startDate 
        GROUP BY kh.keywordWithType.keyword
        HAVING COUNT(kh.keywordWithType.keyword) >= :threshold
        """)
    List<String> findOverusedKeywords(@Param("startDate") LocalDate startDate,
                                      @Param("threshold") int threshold);



    /**
     * 특정 날짜에 사용된 키워드들 조회(해당 날짜만)
     */
    @Query("""
        SELECT DISTINCT kh.keywordWithType.keyword 
        FROM KeywordHistory kh 
        WHERE kh.usedDate = :date 
        """)
    List<String> findKeywordsByUsedDate(@Param("date") LocalDate date);

    @Modifying
    @Query("DELETE FROM KeywordHistory kh WHERE kh.usedDate < :cutoffDate")
    int deleteByUsedDateBefore(@Param("cutoffDate") LocalDate cutoffDate);
}
