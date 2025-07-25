package com.back.domain.news.realNews.controller;

import com.back.domain.news.realNews.dto.NaverNewsDto;
import com.back.domain.news.realNews.dto.RealNewsDto;
import com.back.domain.news.realNews.service.RealNewsService;
import com.back.domain.news.util.NewsConstants;
import com.back.domain.news.util.NewsPageService;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static org.springframework.data.domain.Sort.*;
import static org.springframework.data.domain.Sort.Direction.*;

@RestController
@RequestMapping("api/real-news")
@RequiredArgsConstructor
public class RealNewsController {
    private final RealNewsService realNewsService;
    private final NewsPageService newsPageService;
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

        return newsPageService.getSingleNews(realNewsDto, NewsConstants.REAL_NEWS, id);
    }

    //다건조회(시간순)
    @GetMapping
    public RsData<Page<RealNewsDto>> getRealNewsList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        if (!isValidPageParam(page, size, direction)) {
            return RsData.of(400, "잘못된 페이지 파라미터입니다");
        }

        Direction sortDirection = fromString(direction);
        Sort sortBy = Sort.by(sortDirection, "originCreatedDate");

        Pageable pageable = PageRequest.of(page-1, size, sortBy);
        Page<RealNewsDto> RealNewsPage = realNewsService.getRealNewsList(pageable);

        return newsPageService.getPagedNews(RealNewsPage, NewsConstants.REAL_NEWS);
    }

    //다건조회(검색)
    @GetMapping("/search")
    public RsData<Page<RealNewsDto>> searchRealNewsByTitle(
            @RequestParam String title,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        if (title == null || title.trim().isEmpty()) {
            return RsData.of(400, "검색어를 입력해주세요");
        }

        if (!isValidPageParam(page, size, direction)) {
            return RsData.of(400, "잘못된 페이지 파라미터입니다");
        }

        Direction sortDirection = fromString(direction);
        Sort sortBy = Sort.by(sortDirection, "originCreatedDate");

        Pageable pageable = PageRequest.of(page-1, size, sortBy);
        Page<RealNewsDto> RealNewsPage = realNewsService.searchRealNewsByTitle(title, pageable);

        return newsPageService.getPagedNews(RealNewsPage, NewsConstants.REAL_NEWS);
    }

    private boolean isValidPageParam(int page, int size, String direction) {
        return direction.equals("asc") || direction.equals("desc")
                && page > 0
                && size >= 1 && size <= 100;
    }


}
