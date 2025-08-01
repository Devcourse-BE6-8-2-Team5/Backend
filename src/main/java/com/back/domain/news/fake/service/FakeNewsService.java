package com.back.domain.news.fake.service;
import com.back.domain.news.fake.dto.FakeNewsDto;
import com.back.domain.news.fake.entity.FakeNews;
import com.back.domain.news.fake.repository.FakeNewsRepository;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.global.ai.AiService;
import com.back.global.ai.processor.FakeNewsGeneratorProcessor;
import com.back.global.rateLimiter.RateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FakeNewsService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final FakeNewsRepository fakeNewsRepository;
    private final RealNewsRepository realNewsRepository;
    private final RateLimiter rateLimiter;

    @Qualifier("bucket")
    private final Bucket bucket;


    //가짜뉴스 비동기 생성
    @Async("newsExecutor")
    public CompletableFuture<FakeNewsDto> generateFakeNewsAsync(RealNewsDto realNewsDto) {
        try {
            // Rate limiting
            rateLimiter.waitForRateLimit();

            log.debug("가짜뉴스 생성 시작 - 실제뉴스 ID: {}", realNewsDto.id());

            FakeNewsGeneratorProcessor processor = new FakeNewsGeneratorProcessor(realNewsDto, objectMapper);
            FakeNewsDto result = aiService.process(processor);

            log.debug("가짜뉴스 생성 완료 - 실제뉴스 ID: {}", realNewsDto.id());
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("가짜뉴스 생성 실패 - 실제뉴스 ID: {}", realNewsDto.id(), e);
            throw new RuntimeException("가짜뉴스 생성 실패", e);
        }
    }

    public List<FakeNewsDto> generateFakeNewsBatch(List<RealNewsDto> realNewsDtos) {
        if (realNewsDtos == null || realNewsDtos.isEmpty()) {
            log.warn("생성할 가짜뉴스가 없습니다.");
            return Collections.emptyList();
        }

        log.info("가짜뉴스 배치 생성 시작 (비동기) - 총 {}개", realNewsDtos.size());

        // 모든 뉴스를 비동기로 처리
        List<CompletableFuture<FakeNewsDto>> futures = realNewsDtos.stream()
                .map(this::generateFakeNewsAsync)
                .toList();

        // 모든 결과 수집
        return futures.stream()
                .map(CompletableFuture::join)
                .toList();

    }

    @Transactional
    public List<FakeNewsDto> generateAndSaveAllFakeNews(List<RealNewsDto> realNewsDtos){
        List<FakeNewsDto> fakeNewsDtos = generateFakeNewsBatch(realNewsDtos);
        saveAllFakeNews(fakeNewsDtos);

        return fakeNewsDtos;
    }


    @Transactional
    public void saveAllFakeNews(List<FakeNewsDto> fakeNewsDtos) {
        List<Long> realNewsIds = fakeNewsDtos.stream()
                .map(FakeNewsDto::realNewsId)
                .collect(Collectors.toList());

        // RealNews들을 한 번에 조회
        Map<Long, RealNews> realNewsMap = realNewsRepository.findAllById(realNewsIds)
                .stream()
                .collect(Collectors.toMap(RealNews::getId, Function.identity()));

        // FakeNews 엔티티들 생성 후 저장
        List<FakeNews> fakeNewsList = fakeNewsDtos.stream()
                .filter(dto -> realNewsMap.containsKey(dto.realNewsId())) // 존재하는 realNewsId만 필터링
                .map(dto -> FakeNews.builder()
                        .realNews(realNewsMap.get(dto.realNewsId()))
                        .content(dto.content())
                        .build())
                .collect(Collectors.toList());

        fakeNewsRepository.saveAll(fakeNewsList);
    }

    @Transactional(readOnly = true)
    public FakeNewsDto getFakeNewsByRealNewsId(Long realNewsId) {

        return fakeNewsRepository.findById(realNewsId)
                .map(fakeNews -> new FakeNewsDto(
                        fakeNews.getRealNews().getId(),
                        fakeNews.getContent()))
                .orElseThrow(() -> new IllegalArgumentException("Fake news not found for real news id: " + realNewsId));

    }

    //가짜뉴스 단건 생성
    public FakeNewsDto generateFakeNews(RealNewsDto realNewsDto) {
        FakeNewsGeneratorProcessor processor = new FakeNewsGeneratorProcessor(realNewsDto, objectMapper);
        return aiService.process(processor);
    }

    //단건 저장
    @Transactional
    public void saveFakeNews(FakeNewsDto fakeNewsDto) {
        RealNews mappingNews = realNewsRepository.findById(fakeNewsDto.realNewsId())
                .orElseThrow(() -> new IllegalArgumentException("Real news not found with id: " + fakeNewsDto.realNewsId()));

        FakeNews fakeNews = FakeNews.builder()
                .realNews(mappingNews)
                .content(fakeNewsDto.content())
                .build();

        fakeNewsRepository.save(fakeNews);
    }

    public int count() {
        return (int) fakeNewsRepository.count();
    }
}
