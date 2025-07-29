package com.back.domain.news.common.test.controller;

import com.back.domain.news.common.dto.KeywordGenerationResDto;
import com.back.domain.news.common.service.KeywordCleanupService;
import com.back.domain.news.common.service.KeywordGenerationService;
import com.back.domain.news.fake.dto.FakeNewsDto;
import com.back.domain.news.fake.service.FakeNewsService;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.service.RealNewsService;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class KeywordTestController {

    private final KeywordGenerationService keywordGenerationService;
    private final KeywordCleanupService keywordCleanupService;
    private final FakeNewsService fakeNewsService;
    private final RealNewsService realNewsService;

    @GetMapping("/keywords")
    public KeywordGenerationResDto testKeywords() {
        return keywordGenerationService.generateTodaysKeywords();
    }

    //-1 -> 내일 이전(모든 키워드 삭제)
    // 0 -> 오늘 이전(어제 키워드까지 삭제)
    @GetMapping("/cleanup/{days}")
    public RsData<String> testCleanup(@PathVariable int days) {
        System.out.println("받은 days 값: " + days);

        try {
            keywordCleanupService.adminCleanup(days);
            return new RsData<>(200, "Cleanup successful", null);
        } catch (Exception e) {
            return new RsData<>(500, "Cleanup failed", null);
        }
    }

    @PostMapping("/create/fake")
    public RsData<List<FakeNewsDto>> testCreateFake() {

        List<RealNewsDto> realNewsDtos = realNewsService.getRealNewsListCreatedToday();

        if (realNewsDtos == null || realNewsDtos.isEmpty()) {
            return RsData.of(400, "실제 뉴스 목록이 비어있습니다.");
        }

        try {
            List<FakeNewsDto> fakeNewsDtos = fakeNewsService.generateFakeNewsBatch(realNewsDtos);
            return RsData.of(200, "가짜 뉴스 생성 성공", fakeNewsDtos);
        } catch (Exception e) {
            return RsData.of(500, "가짜 뉴스 생성 실패: " + e.getMessage());
        }
    }

}

