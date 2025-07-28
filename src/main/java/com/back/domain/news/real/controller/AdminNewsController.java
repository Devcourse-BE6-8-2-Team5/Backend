package com.back.domain.news.real.controller;

import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.service.AdminNewsService;
import com.back.domain.news.real.service.RealNewsService;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/news")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNewsController {
    private final AdminNewsService adminNewsService;
    private final RealNewsService realNewsService;



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
            return RsData.of(400, "잘못된 요청입니다. POST 요청이 맞는지 확인하세요");
        } catch (Exception e) {
            return RsData.of(500, "오늘의 뉴스 설정 중 오류가 발생했습니다.");
        }
    }
}
