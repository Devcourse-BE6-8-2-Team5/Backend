package com.back.domain.news.realNews.controller;

import com.back.domain.news.realNews.dto.NaverNewsDto;
import com.back.domain.news.realNews.dto.RealNewsDto;
import com.back.domain.news.realNews.service.RealNewsService;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/real-news")
@RequiredArgsConstructor
public class RealNewsController {
    private final RealNewsService realNewsService;

    //추후 삭제예정 fetch 테스트
    @GetMapping("/fetch")
    public RsData<List<NaverNewsDto>> fetchMetaDataByQuery(@RequestParam String query) {
        List<NaverNewsDto> newsList = realNewsService.fetchNews(query);
        if (newsList.isEmpty()) {
            return RsData.of(404, "뉴스가 없습니다");
        }
        return RsData.of(200, "뉴스 패치 완료", newsList);
    }

    //뉴스 생성
    @PostMapping("/create")
    public RsData<List<RealNewsDto>> createRealNews(@RequestParam String query) {
        List<RealNewsDto> realNewsList = realNewsService.createRealNewsDto(query);

        if (realNewsList.isEmpty()) {
            return RsData.of(404, String.format("'%s' 검색어로 뉴스를 찾을 수 없습니다", query));
        }

        return RsData.of(200, String.format("뉴스 %d건 생성 완료",realNewsList.size()), realNewsList);
    }

    //단건조회
    @GetMapping("/{id}")
    public RsData<RealNewsDto> getRealNewsById(@PathVariable Long id) {
        Optional<RealNewsDto> realNewsDto = realNewsService.getRealNewsDtoById(id);

        return realNewsDto
                .map(dto -> RsData.of(200, "조회 성공", dto))
                .orElse(RsData.of(404, String.format("%d번의 뉴스를 찾을 수 없습니다", id)));
    }




}
