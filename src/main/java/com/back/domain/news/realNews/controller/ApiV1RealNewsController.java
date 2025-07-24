package com.back.domain.news.realNews.controller;

import com.back.domain.news.realNews.dto.NaverNewsDto;
import com.back.domain.news.realNews.dto.RealNewsDto;
import com.back.domain.news.realNews.entity.RealNews;
import com.back.domain.news.realNews.service.RealNewsService;
import com.back.global.rsData.RsData;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/real-news")
@RequiredArgsConstructor
public class ApiV1RealNewsController {
    private final RealNewsService realNewsService;

    //추후 삭제예정
    //http://localhost:8080/api/v1/real-news/fetch?query="삼성전자" << 이런 식으로 뉴스 추출 가능
    @GetMapping("/fetch")
    public RsData<List<NaverNewsDto>> fetchNewsByQuery(@RequestParam String query) {
        List<NaverNewsDto> newsList = realNewsService.fetchNews(query);
        if (newsList.isEmpty()) {
            return RsData.of(404, "뉴스가 없습니다");
        }
        return RsData.of(200, "뉴스 패치 완료", newsList);
    }


    //exceptionhandler로 예외처리 예정
    @PostMapping("/create")
    public RsData<List<RealNewsDto>> createNewsList(@RequestParam String query) {
        List<RealNewsDto> realNewsList = realNewsService.createRealNews(query);

        return RsData.of(200, "뉴스 생성 완료", realNewsList);
    }

}
