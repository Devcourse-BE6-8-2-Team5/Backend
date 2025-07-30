package com.back.domain.news.real.service;

import com.back.domain.news.common.dto.AnalyzedNewsDto;
import com.back.domain.news.common.dto.NaverNewsDto;
import com.back.domain.news.common.dto.NewsDetailDto;
import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.common.service.KeywordGenerationService;
import com.back.domain.news.common.service.NewsAnalysisService;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.mapper.RealNewsMapper;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.domain.news.real.repository.TodayNewsRepository;
import com.back.domain.news.today.entity.TodayNews;
import com.back.global.exception.ServiceException;
import com.back.global.util.HtmlEntityDecoder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNewsService {

    private final NewsDataService newsDataService;
    private final KeywordGenerationService keywordGenerationService;
    private final NewsAnalysisService newsAnalysisService;
    private final static List<String> STATIC_KEYWORD = Arrays.asList("속보", "긴급", "단독", "충격");

    @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 실행
    @Transactional
    public List<RealNewsDto> dailyNewsProcess(){
        List<String> keywords = keywordGenerationService.generateTodaysKeywords().getKeywords();
        //   속보랑 기타키워드 추가
        List<String> additionalKeywords = newsDataService.addKeywords(keywords, STATIC_KEYWORD);

        List<NaverNewsDto> allNews = newsDataService.collectMetaDataFromNaver(keywords);

        List<RealNewsDto> allRealNewsBeforeFilter = newsDataService.createRealNewsDtoByCrawl(allNews);

        List<AnalyzedNewsDto> allRealNewsAfterFilter = newsAnalysisService.filterAndScoreNews(allRealNewsBeforeFilter);

        List<RealNewsDto> selectedNews = newsDataService.selectNewsByScore(allRealNewsAfterFilter);

        return newsDataService.saveAllRealNews(selectedNews);
    }

}
