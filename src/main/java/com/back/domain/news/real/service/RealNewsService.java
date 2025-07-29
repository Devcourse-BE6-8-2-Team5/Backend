package com.back.domain.news.real.service;


import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.mapper.RealNewsMapper;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.domain.news.real.repository.TodayNewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import com.back.domain.news.today.entity.TodayNews;

@Service
@RequiredArgsConstructor
public class RealNewsService {
    private final RealNewsRepository realNewsRepository;
    private final RealNewsMapper realNewsMapper;
    private final TodayNewsRepository todayNewsRepository;

    @Transactional(readOnly = true)
    public Optional<RealNewsDto> getRealNewsDtoById(Long id) {
        return realNewsRepository.findById(id)
                .map(realNewsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<RealNewsDto> getRealNewsList(Pageable pageable) {
        Page<RealNews> realNewsPage = realNewsRepository.findAll(pageable);
        return realNewsPage.map(realNewsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<RealNewsDto> searchRealNewsByTitle(String title, Pageable pageable) {
        Page<RealNews> realNewsPage = realNewsRepository.findByTitleContaining(title, pageable);
        return realNewsPage.map(realNewsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<RealNewsDto> getTodayNews() {
        LocalDate today = LocalDate.now();
        return todayNewsRepository.findBySelectedDate(today)
                .map(TodayNews::getRealNews)        // TodayNews -> RealNews
                .map(realNewsMapper::toDto);

    }
}