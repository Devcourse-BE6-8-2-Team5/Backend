package com.back.domain.news.real.service;


import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.mapper.RealNewsMapper;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.domain.news.real.repository.TodayNewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        Optional<Long> todayNewsId = getTodayNews().map(RealNewsDto::id);

        if (todayNewsId.isPresent()) {
            // 오늘 뉴스가 있다면, 해당 뉴스는 제외하고 나머지 뉴스만 조회
            return realNewsRepository.findByIdNot(todayNewsId.get(), pageable)
                    .map(realNewsMapper::toDto);
        }

        return realNewsRepository.findAll(pageable)
                .map(realNewsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<RealNewsDto> searchRealNewsByTitle(String title, Pageable pageable) {
        Optional<Long> todayNewsId = getTodayNews().map(RealNewsDto::id);
        if (todayNewsId.isPresent()) {
            return realNewsRepository.findByTitleContainingAndIdNot(title, todayNewsId.get(), pageable)
                    .map(realNewsMapper::toDto);
        }

        return realNewsRepository.findByTitleContaining(title, pageable)
                .map(realNewsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<RealNewsDto> getTodayNews() {
        LocalDate today = LocalDate.now();
        return todayNewsRepository.findBySelectedDate(today)
                .map(TodayNews::getRealNews)        // TodayNews -> RealNews
                .map(realNewsMapper::toDto);

    }

    @Transactional(readOnly = true)
    public List<RealNewsDto> getRealNewsListCreatedToday() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        List<RealNews> realNewsList = realNewsRepository.findByCreatedDateBetween(start, end);
        return realNewsList.stream()
                .map(realNewsMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<RealNewsDto> getRealNewsByCategory(NewsCategory category, Pageable pageable) {
        Optional<Long> todayNewsId = getTodayNews().map(RealNewsDto::id);

        if (todayNewsId.isPresent()) {
            // 오늘 뉴스가 있다면, 해당 뉴스는 제외하고 나머지 뉴스만 조회
            return realNewsRepository.findByNewsCategoryAndIdNot(category, todayNewsId.get(), pageable)
                    .map(realNewsMapper::toDto);
        }
        return realNewsRepository.findByNewsCategory(category, pageable)
                .map(realNewsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<RealNewsDto> getRealNewsListExcludingNth(Pageable pageable, int n) {
        Optional<Long> todayNewsId = getTodayNews().map(RealNewsDto::id);

        Pageable unsortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<RealNews> realNewsPage = realNewsRepository.findAllExcludingNth(
                todayNewsId.orElse(null), // 오늘 뉴스 ID가 있다면 제외
                n + 1,
                unsortedPageable);
        return realNewsPage.map(realNewsMapper::toDto);

    }

    @Transactional(readOnly = true)
    public Page<RealNewsDto> searchRealNewsByTitleExcludingNth(String title, Pageable pageable, int n) {
        Optional<Long> todayNewsId = getTodayNews().map(RealNewsDto::id);

        Pageable unsortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<RealNews> page = realNewsRepository.findByTitleExcludingNthCategoryRank(
                title,
                todayNewsId.orElse(null),
                n + 1,
                unsortedPageable
        );

        return page.map(realNewsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<RealNewsDto> getRealNewsListByCategoryExcludingNth(NewsCategory category, Pageable pageable, int n) {
        Optional<Long> todayNewsId = getTodayNews().map(RealNewsDto::id);

        Pageable unsortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<RealNews> page = realNewsRepository.findByCategoryExcludingNth(
                category,
                todayNewsId.orElse(null),
                n + 1,
                unsortedPageable
        );

        return page.map(realNewsMapper::toDto);
    }
}