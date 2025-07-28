package com.back.domain.news.real.service;

import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.domain.news.real.repository.TodayNewsRepository;
import com.back.domain.news.today.entity.TodayNews;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminNewsService {
    private final RealNewsRepository realNewsRepository;
    private final TodayNewsRepository todayNewsRepository;

    @Transactional
    public boolean deleteRealNews(Long newsId) {
        Optional<RealNews> realNewsOpt = realNewsRepository.findById(newsId);

        if (realNewsOpt.isEmpty()) {
            return false;  // 뉴스가 없으면 false 반환
        }

        if (todayNewsRepository.existsById(newsId)) {
            todayNewsRepository.deleteById(newsId);
        }
        // 뉴스 삭제 (FakeNews도 CASCADE로 함께 삭제됨)
        realNewsRepository.deleteById(newsId);
        return true;
    }


    public boolean isAlreadyTodayNews(Long id) {
        return todayNewsRepository.existsById(id);
    }

    @Transactional
    public void setTodayNews(Long id) {
        RealNews realNews = realNewsRepository.findById(id).
                orElseThrow(() -> new IllegalArgumentException("해당 ID의 뉴스가 존재하지 않습니다. ID: " + id));

        LocalDate today = LocalDate.now();
        todayNewsRepository.deleteBySelectedDate(today);

        // 5. 새로운 오늘의 뉴스 생성
        TodayNews todayNews = TodayNews.builder()
                .selectedDate(today)
                .realNews(realNews)
                .build();

        todayNewsRepository.save(todayNews);
    }
}
