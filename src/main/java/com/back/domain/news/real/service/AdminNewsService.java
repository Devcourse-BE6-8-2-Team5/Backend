package com.back.domain.news.real.service;

import com.back.domain.news.common.dto.AnalyzedNewsDto;
import com.back.domain.news.common.dto.NaverNewsDto;
import com.back.domain.news.common.service.KeywordGenerationService;
import com.back.domain.news.common.service.AnalysisNewsService;
import com.back.domain.news.real.dto.RealNewsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNewsService {

    private final NewsDataService newsDataService;
    private final KeywordGenerationService keywordGenerationService;
    private final AnalysisNewsService analysisNewsService;
    private final static List<String> STATIC_KEYWORD = Arrays.asList("속보", "긴급", "단독", "충격");

    @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 실행
    @Transactional
    public void dailyNewsProcess(){
        List<String> keywords = keywordGenerationService.generateTodaysKeywords().getKeywords();
        //   속보랑 기타키워드 추가
        List<String> newsKeywords = newsDataService.addKeywords(keywords, STATIC_KEYWORD);

        List<NaverNewsDto> newsKeywordsAfterAdd = newsDataService.collectMetaDataFromNaver(newsKeywords);

        List<RealNewsDto> NewsBeforeFilter = newsDataService.createRealNewsDtoByCrawl(newsKeywordsAfterAdd);

        List<RealNewsDto> NewsRemovedDuplicateTitles = newsDataService.removeDuplicateTitles(NewsBeforeFilter);


        List<AnalyzedNewsDto> newsAfterFilter = analysisNewsService.filterAndScoreNews(NewsRemovedDuplicateTitles);

        List<RealNewsDto> selectedNews = newsDataService.selectNewsByScore(newsAfterFilter);

        newsDataService.saveAllRealNews(selectedNews);

    }

}
