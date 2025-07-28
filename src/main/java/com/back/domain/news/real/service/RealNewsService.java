package com.back.domain.news.real.service;

import com.back.domain.news.common.dto.NaverNewsDto;
import com.back.domain.news.common.dto.NewsDetailDto;
import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.mapper.RealNewsMapper;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.global.util.HtmlEntityDecoder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class RealNewsService {
    private final RealNewsRepository realNewsRepository;
    private final RealNewsMapper realNewsMapper;

    @Transactional(readOnly = true)
    public Optional<RealNewsDto> getRealNewsDtoById(Long id) {
        return realNewsRepository.findById(id)
                .map(realNewsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<RealNewsDto> getRealNewsList(Pageable pageable) {
        Page<RealNews> realNewsPage = realNewsRepository.findAll(pageable);
        return realNewsPage.map(realNewsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<RealNewsDto> searchRealNewsByTitle(String title, Pageable pageable) {
        Page<RealNews> realNewsPage = realNewsRepository.findByTitleContaining(title, pageable);
        return realNewsPage.map(realNewsMapper::toDto);
    }
}