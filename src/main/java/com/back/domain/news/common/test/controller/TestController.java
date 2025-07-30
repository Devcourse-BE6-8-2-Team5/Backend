package com.back.domain.news.common.test.controller;

import com.back.domain.news.common.dto.AnalyzedNewsDto;
import com.back.domain.news.common.dto.KeywordGenerationResDto;
import com.back.domain.news.common.dto.NaverNewsDto;
import com.back.domain.news.common.service.KeywordCleanupService;
import com.back.domain.news.common.service.KeywordGenerationService;
import com.back.domain.news.fake.dto.FakeNewsDto;
import com.back.domain.news.fake.service.FakeNewsService;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.service.AdminNewsService;
import com.back.domain.news.real.service.RealNewsService;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final KeywordGenerationService keywordGenerationService;
    private final KeywordCleanupService keywordCleanupService;
    private final FakeNewsService fakeNewsService;
    private final RealNewsService realNewsService;
    private final AdminNewsService adminNewsService;

    @GetMapping("/keywords")
    public KeywordGenerationResDto testKeywords() {
        return keywordGenerationService.generateTodaysKeywords();
    }

    //-1 -> 내일 이전(모든 키워드 삭제)
    // 0 -> 오늘 이전(어제 키워드까지 삭제)
    @GetMapping("/cleanup/{days}")
    public RsData<String> testCleanup(@PathVariable int days) {
        log.debug("testCleanup days: {}", days);

        try {
            keywordCleanupService.adminCleanup(days);
            return new RsData<>(200, "Cleanup successful", null);
        } catch (Exception e) {
            log.error("Cleanup failed: {}", e.getMessage());
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
            List<FakeNewsDto> fakeNewsDtos = fakeNewsService.generateAndSaveAllFakeNews(realNewsDtos);

            return RsData.of(200, "가짜 뉴스 생성 성공", fakeNewsDtos);
        } catch (Exception e) {
            return RsData.of(500, "가짜 뉴스 생성 실패: " + e.getMessage());
        }
    }

    @GetMapping("/fake/{id}")
    public FakeNewsDto testGetFakeNews(@PathVariable Long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID는 1 이상의 숫자여야 합니다.");
        }

        return fakeNewsService.getFakeNewsByRealNewsId(id);
    }
    @GetMapping("/fetch")
    public RsData<List<NaverNewsDto>> testFetchNews(@RequestParam String query) {
        try {
            // 네이버 API 호출
            List<NaverNewsDto> news = adminNewsService.fetchNews(query);

            // 속도 제한 준수
            Thread.sleep(1000);

            return RsData.of(200, "네이버 뉴스 조회 성공", news);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return RsData.of(500, "네이버 뉴스 조회 중단됨: " + e.getMessage());
        } catch (Exception e) {
            log.error("네이버 뉴스 조회 실패: {}", e.getMessage());
            return RsData.of(500, "네이버 뉴스 조회 실패: " + e.getMessage());
        }
    }



    @GetMapping("/keyword/create")
    public RsData<List<String>> testCreateKeyword() {
        try {
            KeywordGenerationResDto keywords = keywordGenerationService.generateTodaysKeywords();
            List<String> keywordList = keywords.getKeywords();
//            List<String> prefixes = List.of("속보", "긴급", "단독");
//
//            keywordList = Stream
//                    .concat(keywordList.stream(), prefixes.stream())
//                    .toList();

            return RsData.of(200, "키워드 생성 성공", keywordList);
        } catch (Exception e) {
            log.error("키워드 생성 실패: {}", e.getMessage());
            return RsData.of(500, "키워드 생성 실패: " + e.getMessage());
        }
    }

}

