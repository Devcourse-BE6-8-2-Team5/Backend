package com.back.dev.service;

import com.back.domain.news.common.dto.AnalyzedNewsDto;
import com.back.domain.news.common.dto.NaverNewsDto;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.service.NewsAnalysisService;
import com.back.domain.news.real.service.NewsDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Profile("!prod")
@RequiredArgsConstructor
@Service
public class DevTestNewsService {
    private final NewsDataService newsDataService;
    private final NewsAnalysisService newsAnalysisService;

    public List<RealNewsDto> testNewsDataService() {
        List<String> newsKeywordsAfterAdd = List.of("AI");

        List<NaverNewsDto> newsMetaData = newsDataService.collectMetaDataFromNaver(newsKeywordsAfterAdd);

        List<RealNewsDto> newsAfterCrwal = newsDataService.createRealNewsDtoByCrawl(newsMetaData);

        List<AnalyzedNewsDto> newsAfterFilter = newsAnalysisService.filterAndScoreNews(newsAfterCrwal);

        List<RealNewsDto> selectedNews = newsDataService.selectNewsByScore(newsAfterFilter);

        List<RealNewsDto> savedNews = newsDataService.saveAllRealNews(selectedNews);

        return savedNews;
    }
}
