package com.back.domain.news.real.service;

import com.back.domain.news.common.dto.AnalyzedNewsDto;
import com.back.domain.news.common.dto.NaverNewsDto;
import com.back.domain.news.common.service.KeywordGenerationService;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.event.RealNewsCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNewsService {

    private final NewsDataService newsDataService;
    private final KeywordGenerationService keywordGenerationService;
    private final NewsAnalysisService newsAnalysisService;
    private final static List<String> STATIC_KEYWORD = Arrays.asList("속보", "긴급", "단독");
    private final ApplicationEventPublisher publisher;



    @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 실행
    @Transactional
    public void dailyNewsProcess(){
        List<String> keywords = keywordGenerationService.generateTodaysKeywords().getKeywords();

        List<String> newsKeywords = newsDataService.addKeywords(keywords, STATIC_KEYWORD);

        List<NaverNewsDto> newsKeywordsAfterAdd = newsDataService.collectMetaDataFromNaver(newsKeywords);

        List<RealNewsDto> newsAfterCrwal = newsDataService.createRealNewsDtoByCrawl(newsKeywordsAfterAdd);

        List<AnalyzedNewsDto> newsAfterFilter = newsAnalysisService.filterAndScoreNews(newsAfterCrwal);

        List<RealNewsDto> selectedNews = newsDataService.selectNewsByScore(newsAfterFilter);

        List<RealNewsDto> savedNews = newsDataService.saveAllRealNews(selectedNews);

        if(savedNews.isEmpty()) {
            log.warn("저장된 뉴스가 없습니다. 오늘의 뉴스 수집이 실패했을 수 있습니다.");
            return;
        }
        newsDataService.setTodayNews(savedNews.getFirst().id());

        List<Long> realNewsIds = savedNews.stream()
                .map(RealNewsDto::id)
                .filter(Objects::nonNull) // null 체크
                .toList();

        // 트랜잭션 커밋 이후에 이벤트 발행
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publisher.publishEvent(new RealNewsCreatedEvent(realNewsIds));
            }
        });

    }

}
