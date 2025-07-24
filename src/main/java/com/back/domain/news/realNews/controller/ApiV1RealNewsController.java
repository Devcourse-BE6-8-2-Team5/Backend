package com.back.domain.news.realNews.controller;

import com.back.domain.news.realNews.dto.NaverNewsDto;
import com.back.domain.news.realNews.dto.RealNewsDto;
import com.back.domain.news.realNews.entity.RealNews;
import com.back.domain.news.realNews.service.RealNewsService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/real-news")
@RequiredArgsConstructor
public class ApiV1RealNewsController {
    private final RealNewsService realNewsService;

    //추후 글로벌에 추가되면 삭제 예정
    record RsData<T>(
            String resultCode,
            @JsonIgnore
            int statusCode,
            String msg,
            T data
    ) {
        RsData(String resultCode, String msg) {
            this(resultCode, msg, null);
        }

        RsData(String resultCode, String msg, T data) {
            this(resultCode, Integer.parseInt(resultCode.split("-", 2)[0]), msg, data);
        }
    }


    //추후 삭제예정
    //http://localhost:8080/api/v1/real-news/fetch?query="삼성전자" << 이런 식으로 뉴스 추출 가능
    @GetMapping("/fetch")
    public RsData<List<NaverNewsDto>> fetchNewsByQuery(@RequestParam String query) throws JsonProcessingException {
        List<NaverNewsDto> newsList = realNewsService.fetchNews(query);
        if (newsList.isEmpty()) {
            return new RsData<>("404-1", "뉴스가 없습니다.");
        }
        return new RsData<>("200", "뉴스 패치 완료", newsList);
    }


    //exceptionhandler로 예외처리 예정
    @GetMapping("/create")
    public RsData<List<RealNewsDto>> createNewsList(@RequestParam String query) throws IOException {
        List<RealNewsDto> realNewsList = realNewsService.createRealNews(query);

        return new RsData<>("200", "뉴스 생성 완료", realNewsList);
    }

}
