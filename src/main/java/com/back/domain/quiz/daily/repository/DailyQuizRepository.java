package com.back.domain.quiz.daily.repository;

import com.back.domain.quiz.daily.entity.DailyQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DailyQuizRepository extends JpaRepository<DailyQuiz, Long> {
    @Query("""
            SELECT dq 
            FROM DailyQuiz dq
            JOIN FETCH dq.detailQuiz 
            WHERE dq.todayNews.id = :todayNewsId
            """)
    List<DailyQuiz> findByTodayNewsId(@Param("todayNewsId") Long todayNewsId);
}
