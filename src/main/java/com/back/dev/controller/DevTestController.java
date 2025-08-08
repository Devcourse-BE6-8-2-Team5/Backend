package com.back.dev.controller;

import com.back.dev.service.DevTestNewsService;
import com.back.domain.news.common.dto.NaverNewsDto;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Profile("!prod")
@RequiredArgsConstructor
@RestController
@RequestMapping("/test")
public class DevTestController {

    private final DevTestNewsService devTestNewsService;

    @GetMapping("/news")
    public RsData<List<RealNewsDto>> testCreateNews(){

        List<RealNewsDto> testNews;

        try{
            testNews =  devTestNewsService.testNewsDataService();
        } catch(Exception e){
            return RsData.of(500, "테스트 뉴스 생성 실패: " + e.getMessage());
        }

        return RsData.of(200, "테스트 뉴스 생성 완료", testNews);
    }

    @GetMapping("/fetch")
    public RsData<List<Map<String, String>>> testFetch(@RequestParam String query){
        List<NaverNewsDto> testNews = devTestNewsService.fetchNews(query);

        List<String> titles = testNews.stream()
                .map(NaverNewsDto::title)
                .toList();

        List<Map<String,String>> extractdupKeywords = new ArrayList<>();
        for(String title : titles){
            List<String> keywords = devTestNewsService.extractKeywords(title);
            Map<String, String> map = new HashMap<>();
            map.put("title", title);
            map.put("keywords", String.join(",", keywords));
            extractdupKeywords.add(map);
        }
        return RsData.of(200, "테스트 뉴스 메타데이터 생성 완료", extractdupKeywords);
    }

    @GetMapping("/keyword")
    public List<String> getExtractedWord(@RequestParam String keyword){

        return devTestNewsService.extractKeywords(keyword);
    }
}
