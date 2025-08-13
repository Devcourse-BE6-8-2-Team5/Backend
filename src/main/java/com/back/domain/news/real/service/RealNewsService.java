package com.back.domain.news.real.service;


import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.mapper.RealNewsMapper;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.domain.news.today.repository.TodayNewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

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
        Long excludedId = getTodayNewsOrRecent();

        return realNewsRepository.findByIdNotOrderByCreatedDateDesc(excludedId, pageable)
                .map(realNewsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<RealNewsDto> searchRealNewsByTitle(String title, Pageable pageable) {
        Long excludedId = getTodayNewsOrRecent();

        return realNewsRepository.findByTitleContainingAndIdNotOrderByCreatedDateDesc(title, excludedId, pageable)
                .map(realNewsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<RealNewsDto> getTodayNews() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        return todayNewsRepository.findBySelectedDate(today)
                .map(TodayNews::getRealNews)        // TodayNews -> RealNews
                .map(realNewsMapper::toDto);

    }

    @Transactional(readOnly = true)
    public List<RealNewsDto> getRealNewsListCreatedToday() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        List<RealNews> realNewsList = realNewsRepository.findByCreatedDateBetweenOrderByCreatedDateDesc(start, end);
        return realNewsList.stream()
                .map(realNewsMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<RealNewsDto> getAllRealNewsByCategory(NewsCategory category, Pageable pageable) {
        Long excludedId = getTodayNewsOrRecent();

        return realNewsRepository.findByNewsCategoryAndIdNotOrderByCreatedDateDesc(category, excludedId, pageable)
                .map(realNewsMapper::toDto);


    }

    @Transactional(readOnly = true)
    public Page<RealNewsDto> getRealNewsListExcludingNth(Pageable pageable, int n) {
        Long excludedId = getTodayNewsOrRecent();

        Pageable unsortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<RealNews> realNewsPage = realNewsRepository.findAllExcludingNth(
                excludedId,
                n + 1,
                unsortedPageable);

        return realNewsPage.map(realNewsMapper::toDto);

    }

    @Transactional(readOnly = true)
    public Page<RealNewsDto> searchRealNewsByTitleExcludingNth(String title, Pageable pageable, int n) {
        Long excludedId = getTodayNewsOrRecent();

        Pageable unsortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<RealNews> page = realNewsRepository.findByTitleExcludingNthCategoryRank(
                title,
                excludedId,
                n + 1,
                unsortedPageable
        );

        return page.map(realNewsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<RealNewsDto> getRealNewsListByCategoryExcludingNth(NewsCategory category, Pageable pageable, int n) {
        Long excludedId = getTodayNewsOrRecent();

        Pageable unsortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        Page<RealNews> page = realNewsRepository.findByCategoryExcludingNth(
                category,
                excludedId,
                n + 1,
                unsortedPageable
        );

        return page.map(realNewsMapper::toDto);
    }

    public Long getTodayNewsOrRecent() {
        return todayNewsRepository.findTopByOrderBySelectedDateDesc()
                .map(TodayNews::getId)
                .orElse(-1L);
    }
}