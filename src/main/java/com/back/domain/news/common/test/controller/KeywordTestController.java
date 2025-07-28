package com.back.domain.news.common.test.controller;

import com.back.domain.news.common.dto.KeywordGenerationResDto;
import com.back.domain.news.common.service.KeywordCleanupService;
import com.back.domain.news.common.service.KeywordGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class KeywordTestController {

    private final KeywordGenerationService keywordGenerationService;

    @GetMapping("/test/keywords")
    public KeywordGenerationResDto testKeywords() {
        return keywordGenerationService.generateTodaysKeywords();
    }

}