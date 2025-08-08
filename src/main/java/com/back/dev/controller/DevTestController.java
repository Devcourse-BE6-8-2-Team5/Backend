package com.back.dev.controller;

import com.back.dev.service.DevTestNewsService;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

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
}
