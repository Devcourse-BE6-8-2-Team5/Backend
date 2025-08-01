package com.back.domain.member.quizhistory.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.quizhistory.entity.QuizHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizHistoryRepository extends JpaRepository<QuizHistory, Long> {
    List<QuizHistory> findAllByMemberOrderByCreatedDateDesc(Member actor);
    List<QuizHistory> findByMemberOrderByCreatedDateDesc(Member actor);

    List<QuizHistory> findByMember(Member actor);
}
