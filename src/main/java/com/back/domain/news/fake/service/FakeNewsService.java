package com.back.domain.news.fake.service;

import com.back.domain.news.fake.dto.FakeNewsDto;
import com.back.domain.news.fake.entity.FakeNews;
import com.back.domain.news.fake.repository.FakeNewsRepository;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.global.ai.AiService;
import com.back.global.ai.processor.FakeNewsGeneratorProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FakeNewsService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final FakeNewsRepository fakeNewsRepository;
    private final RealNewsRepository realNewsRepository;


    public FakeNewsDto generateFakeNews(RealNewsDto realNewsDto) {
        FakeNewsGeneratorProcessor processor = new FakeNewsGeneratorProcessor(realNewsDto, objectMapper);
        return aiService.process(processor);
    }

    public List<FakeNewsDto> generateFakeNewsBatch(List<RealNewsDto> realNewsDtos) {
        return realNewsDtos.stream()
                .map(this::generateFakeNews)
                .collect(Collectors.toList());
    }



    @Transactional
    public List<FakeNewsDto> generateAndSaveAllFakeNews(List<RealNewsDto> realNewsDtos){
        List<FakeNewsDto> fakeNewsDtos = generateFakeNewsBatch(realNewsDtos);
        saveAllFakeNews(fakeNewsDtos);

        return fakeNewsDtos;
    }

    @Transactional
    public void saveFakeNews(FakeNewsDto fakeNewsDto) {
        RealNews mappingNews = realNewsRepository.findById(fakeNewsDto.realNewsId())
                .orElseThrow(() -> new IllegalArgumentException("Real news not found with id: " + fakeNewsDto.realNewsId()));

        FakeNews fakeNews = FakeNews.builder()
                .realNews(mappingNews)
                .title(fakeNewsDto.title())
                .content(fakeNewsDto.content())
                .build();

        fakeNewsRepository.save(fakeNews);
    }

    @Transactional
    public void saveAllFakeNews(List<FakeNewsDto> fakeNewsDtos) {
        List<Long> realNewsIds = fakeNewsDtos.stream()
                .map(FakeNewsDto::realNewsId)
                .collect(Collectors.toList());

        // 2. RealNews들을 한 번에 조회
        Map<Long, RealNews> realNewsMap = realNewsRepository.findAllById(realNewsIds)
                .stream()
                .collect(Collectors.toMap(RealNews::getId, Function.identity()));

        // 3. FakeNews 엔티티들 생성 후 저장
        List<FakeNews> fakeNewsList = fakeNewsDtos.stream()
                .filter(dto -> realNewsMap.containsKey(dto.realNewsId())) // 존재하는 realNewsId만 필터링
                .map(dto -> FakeNews.builder()
                        .realNews(realNewsMap.get(dto.realNewsId()))
                        .title(dto.title())
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
                        fakeNews.getTitle(),
                        fakeNews.getContent()))
                .orElseThrow(() -> new IllegalArgumentException("Fake news not found for real news id: " + realNewsId));

    }
}
