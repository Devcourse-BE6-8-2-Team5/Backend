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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static org.springframework.data.domain.Sort.*;
import static org.springframework.data.domain.Sort.Direction.*;

@Tag(name = "RealNewsController", description = "Real News API")
@RestController
@RequestMapping("api/news")
@RequiredArgsConstructor
public class RealNewsController {
    private final RealNewsService realNewsService;
    private final NewsPageService newsPageService;



    //단건조회
    @Operation(summary = "단건 뉴스 조회", description = "ID로 단건 뉴스를 조회합니다.")
    @GetMapping("/{newsId}")
    public RsData<RealNewsDto> getRealNewsById(@PathVariable Long newsId) {

        if (newsId == null || newsId <= 0) {
            return RsData.of(400, "잘못된 뉴스 ID입니다. 1 이상의 숫자를 입력해주세요.");
        }

        Optional<RealNewsDto> realNewsDto = realNewsService.getRealNewsDtoById(newsId);

        if (realNewsDto.isEmpty()) {
            return RsData.of(404,
                    String.format("ID %d에 해당하는 뉴스를 찾을 수 없습니다. 올바른 뉴스 ID인지 확인해주세요.", newsId));
        }


        return newsPageService.getSingleNews(realNewsDto, NewsType.REAL, newsId);
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

    private boolean isValidPageParam(int page, int size, String direction) {
        return (direction.equals("asc") || direction.equals("desc"))
                && page > 0
                && size >= 1 && size <= 100;
    }

}
