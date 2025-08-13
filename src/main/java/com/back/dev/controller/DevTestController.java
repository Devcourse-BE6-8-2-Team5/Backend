package com.back.dev.controller;

import com.back.dev.service.DevTestNewsService;
import com.back.domain.news.common.dto.NaverNewsDto;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.service.NewsDataService;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Profile("!prod")
@RequiredArgsConstructor
@RestController
@RequestMapping("/ntest")
public class DevTestController {

    private final DevTestNewsService devTestNewsService;
    private final NewsDataService newsDataService;

    @GetMapping("/news")
    public RsData<List<NaverNewsDto>> testCreateNews(){

        List<NaverNewsDto> testNews;

        try{
            testNews =  devTestNewsService.testNewsDataService();
        } catch(Exception e){
            return RsData.of(500, "테스트 뉴스 생성 실패: " + e.getMessage());
        }

        return RsData.of(200, "테스트 뉴스 생성 완료", testNews);
    }

    @GetMapping("/fetch")
    public RsData<List<NaverNewsDto>> testFetch(@RequestParam String query){
        List<NaverNewsDto> testNews = devTestNewsService.fetchNews(query);

        return RsData.of(200, "테스트 뉴스 메타데이터 생성 완료", testNews);
    }



}
