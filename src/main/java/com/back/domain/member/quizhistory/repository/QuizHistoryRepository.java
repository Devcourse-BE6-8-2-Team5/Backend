package com.back.domain.member.quizhistory.repository;

import com.back.domain.member.quizhistory.entity.QuizHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizHistoryRepository extends JpaRepository<QuizHistory, Long> {
}
