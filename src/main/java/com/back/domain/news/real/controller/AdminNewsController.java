package com.back.domain.news.real.controller;

import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.common.enums.NewsType;
import com.back.domain.news.common.service.NewsPageService;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.service.AdminNewsService;
import com.back.domain.news.real.service.RealNewsService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.springframework.data.domain.Sort.Direction.fromString;

@RestController
@RequestMapping("/api/admin/news")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNewsController {
    private final AdminNewsService adminNewsService;
    private final RealNewsService realNewsService;
    private final NewsPageService newsPageService;


//     뉴스 배치 프로세서
    @GetMapping("/process")
    public RsData<List<RealNewsDto>> newsProcess() {
        try {
            List<RealNewsDto> result = adminNewsService.dailyNewsProcess();

            return RsData.of(200, "성공", result);
        } catch (Exception e) {
            return RsData.of(500, "실패: " + e.getMessage());
        }
    }

    //뉴스 생성
    @Operation(summary = "뉴스 생성", description = "네이버 뉴스 API와 데이터 파싱을 통해 뉴스를 생성합니다.")
    @PostMapping("/create")
    public RsData<List<RealNewsDto>> createRealNews(@RequestParam String query) {
        List<RealNewsDto> realNewsList = adminNewsService.createRealNewsDto(query);

        if (realNewsList.isEmpty()) {
            return RsData.of(404, String.format("'%s' 검색어로 뉴스를 찾을 수 없습니다", query));
        }

        return RsData.of(200, String.format("뉴스 %d건 생성 완료",realNewsList.size()), realNewsList);
    }


    //뉴스 삭제
    @Operation(summary = "뉴스 삭제", description = "ID로 뉴스를 삭제합니다.")
    @DeleteMapping("/{newsId}")

    public RsData<Void> deleteRealNews(@PathVariable Long newsId) {
        boolean deleted = adminNewsService.deleteRealNews(newsId);

        if(!deleted){
            return RsData.of(404, String.format("ID %d에 해당하는 뉴스가 존재하지 않습니다", newsId));
        }

        return RsData.of(200, String.format("%d번 뉴스 삭제 완료", newsId));
    }

    @Operation(summary = "오늘의 뉴스 설정", description = "오늘의 뉴스를 설정합니다.")
    @PostMapping("/today/select/{newsId}")
    public RsData<RealNewsDto> setTodayNews(@PathVariable Long newsId) {

        try {
            // 1. 뉴스 존재 여부 확인
            Optional<RealNewsDto> realNewsDto = realNewsService.getRealNewsDtoById(newsId);
            if (realNewsDto.isEmpty()) {
                return RsData.of(404, String.format("ID %d에 해당하는 뉴스가 존재하지 않습니다", newsId));
            }
            // 2. 이미 오늘의 뉴스인지 확인 (서비스에서 처리)
            if (adminNewsService.isAlreadyTodayNews(newsId)) {
                return RsData.of(400, "이미 오늘의 뉴스로 설정되어 있습니다.", realNewsDto.get());
            }

            adminNewsService.setTodayNews(newsId);

            return RsData.of(200, "오늘의 뉴스가 설정되었습니다.", realNewsDto.get());

        } catch (IllegalArgumentException e) {
            return RsData.of(400, e.getMessage());
        } catch (Exception e) {
            return RsData.of(500, "오늘의 뉴스 설정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

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

    @Operation(summary = "오늘의 뉴스 조회", description = "선정된 오늘의 뉴스를 조회합니다.")
    @GetMapping("/today")
    public RsData<RealNewsDto> getTodayNews() {
        Optional<RealNewsDto> todayNews = realNewsService.getTodayNews();

        if (todayNews.isEmpty()) {
            return RsData.of(404, "조회할 뉴스가 없습니다.");
        }

        return newsPageService.getSingleNews(todayNews, NewsType.REAL, todayNews.get().id());
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

        Sort.Direction sortDirection = fromString(direction);
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

        Sort.Direction sortDirection = fromString(direction);
        Sort sortBy = Sort.by(sortDirection, "originCreatedDate");

        Pageable pageable = PageRequest.of(page-1, size, sortBy);
        Page<RealNewsDto> RealNewsPage = realNewsService.searchRealNewsByTitle(title, pageable);

        return newsPageService.getPagedNews(RealNewsPage, NewsType.REAL);
    }

    @Operation(summary = "카테고리별 뉴스 조회", description = "카테고리별로 뉴스를 조회합니다")
    @GetMapping("/category/{category}")
    public RsData<Page<RealNewsDto>> getRealNewsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        if (!isValidPageParam(page, size, direction)) {
            return RsData.of(400, "잘못된 페이지 파라미터입니다");
        }

        NewsCategory newsCategory;

        try {
            newsCategory = NewsCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RsData.of(400, "올바르지 않은 카테고리입니다. 사용 가능한 카테고리: " +
                    Arrays.toString(NewsCategory.values()));
        }

        Sort.Direction sortDirection = fromString(direction);
        Sort sortBy = Sort.by(sortDirection, "originCreatedDate");

        Pageable pageable = PageRequest.of(page-1, size, sortBy);
        Page<RealNewsDto> realNewsPage = realNewsService.getRealNewsByCategory(newsCategory, pageable);

        return newsPageService.getPagedNews(realNewsPage, NewsType.REAL);
    }

    private boolean isValidPageParam(int page, int size, String direction) {
        return (direction.equals("asc") || direction.equals("desc"))
                && page > 0
                && size >= 1 && size <= 100;
    }
}
