package com.back.domain.news.real.controller;

import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.service.RealNewsService;
import com.back.domain.news.common.service.NewsPageService;
import com.back.domain.news.common.enums.NewsType;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "RealNewsController", description = "Real News API")
@RestController
@RequestMapping("api/real-news")
@RequiredArgsConstructor
public class RealNewsController {
    private final RealNewsService realNewsService;
    private final NewsPageService newsPageService;


    //뉴스 생성
    @Operation(summary = "뉴스 생성", description = "네이버 뉴스 API와 데이터 파싱을 통해 뉴스를 생성합니다.")
    @PostMapping("/create")
    public RsData<List<RealNewsDto>> createRealNews(@RequestParam String query) {
        List<RealNewsDto> realNewsList = realNewsService.createRealNewsDto(query);

        if (realNewsList.isEmpty()) {
            return RsData.of(404, String.format("'%s' 검색어로 뉴스를 찾을 수 없습니다", query));
        }

        return RsData.of(200, String.format("뉴스 %d건 생성 완료",realNewsList.size()), realNewsList);
    }

    //단건조회
    @Operation(summary = "단건 뉴스 조회", description = "ID로 단건 뉴스를 조회합니다.")
    @GetMapping("/{id}")
    public RsData<RealNewsDto> getRealNewsById(@PathVariable Long id) {
        Optional<RealNewsDto> realNewsDto = realNewsService.getRealNewsDtoById(id);

        return newsPageService.getSingleNews(realNewsDto, NewsType.REAL, id);
    }

    //다건조회(시간순)
    @Operation(summary = "다건 뉴스 조회", description = "페이지네이션을 통해 시간순으로 다건 뉴스를 조회합니다.")
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
        Page<RealNewsDto> realNewsPage = realNewsService.getRealNewsList(pageable);

        return newsPageService.getPagedNews(realNewsPage, NewsType.REAL);
    }

    //다건조회(검색)
    @Operation(summary = "뉴스 검색", description = "제목으로 뉴스를 검색합니다.")
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

        return newsPageService.getPagedNews(RealNewsPage, NewsType.REAL);
    }

    //뉴스 삭제
    @Operation(summary = "뉴스 삭제", description = "ID로 뉴스를 삭제합니다.")
    @DeleteMapping("/{id}")
    public RsData<Void> deleteRealNews(@PathVariable Long id) {
        boolean deleted = realNewsService.deleteRealNews(id);

        if(!deleted){
            return RsData.of(404, String.format("ID %d에 해당하는 뉴스가 존재하지 않습니다", id));
        }

        return RsData.of(200, String.format("%d번 뉴스 삭제 완료", id));
    }

    private boolean isValidPageParam(int page, int size, String direction) {
        return (direction.equals("asc") || direction.equals("desc"))
                && page > 0
                && size >= 1 && size <= 100;
    }

}
