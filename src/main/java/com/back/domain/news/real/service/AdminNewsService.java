package com.back.domain.news.real.service;

import com.back.domain.news.common.dto.AnalyzedNewsDto;
import com.back.domain.news.common.dto.NaverNewsDto;
import com.back.domain.news.common.dto.NewsDetailDto;
import com.back.domain.news.common.enums.NewsCategory;
import com.back.domain.news.common.service.KeywordGenerationService;
import com.back.domain.news.common.service.NewsAnalysisService;
import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.entity.RealNews;
import com.back.domain.news.real.mapper.RealNewsMapper;
import com.back.domain.news.real.repository.RealNewsRepository;
import com.back.domain.news.real.repository.TodayNewsRepository;
import com.back.domain.news.today.entity.TodayNews;
import com.back.global.exception.ServiceException;
import com.back.global.util.HtmlEntityDecoder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNewsService {
    private final RealNewsRepository realNewsRepository;
    private final TodayNewsRepository todayNewsRepository;
    private final RealNewsMapper realNewsMapper;
    private final NewsAnalysisService newsAnalysisService;
    private final KeywordGenerationService keywordGenerationService;

    // HTTP 요청을 보내기 위한 Spring의 HTTP 클라이언트(외부 API 호출 시 사용)
    private final RestTemplate restTemplate;

    @Value("${NAVER_CLIENT_ID}")
    private String clientId;

    @Value("${NAVER_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${naver.news.display}")
    private int newsDisplayCount;

    @Value("${naver.news.sort:sim}")
    private String newsSortOrder;

    @Value("${naver.crawling.delay}")
    private int crawlingDelay;

    @Value("${naver.base-url}")
    private String naverUrl;

    // 서비스 초기화 시 설정값 검증
    @PostConstruct
    public void validateConfig(){
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("NAVER_CLIENT_ID가 설정되지 않았습니다.");
        }
        if (clientSecret == null || clientSecret.isEmpty()) {
            throw new IllegalArgumentException("NAVER_CLIENT_SECRET가 설정되지 않았습니다.");
        }
        if (newsDisplayCount < 1 || newsDisplayCount >10) {
            throw new IllegalArgumentException("NAVER_NEWS_DISPLAY_COUNT는 1에서 10 사이의 값이어야 합니다.");
        }
    }

    @Transactional
    public List<RealNewsDto> dailyNewsProcess(){
        List<String> keywords = keywordGenerationService.generateTodaysKeywords().getKeywords();
        //   속보랑 기타키워드 추가

        List<NaverNewsDto> allNews = collectMetaDataFromNaver(keywords);

        List<RealNewsDto> allRealNewsBeforeFilter = createRealNewsDtoByCrawl(allNews);

        List<AnalyzedNewsDto> allRealNewsAfterFilter = newsAnalysisService.filterAndScoreNews(allRealNewsBeforeFilter);

        List<RealNewsDto> selectedNews = selectNewsByScore(allRealNewsAfterFilter);

        return saveAllRealNews(selectedNews);
    }
    // RealNewsDto를 생성하는 메서드
    @Transactional
    public List<RealNewsDto> createRealNewsDto(String query) {

        try {
            List<NaverNewsDto> naverMetaDataList = fetchNews(query);
            List<RealNewsDto> realNewsDtoList = new ArrayList<>();

            for (NaverNewsDto naverMetaData : naverMetaDataList) {
                Optional<NewsDetailDto> newsDetailData = crawladditionalInfo(naverMetaData.link());

                if (newsDetailData.isEmpty()) {
                    // 크롤링 실패 시 해당 뉴스는 건너뜀
                    continue;
                }

                RealNewsDto realNewsDto = makeRealNewsFromInfo(naverMetaData, newsDetailData.get());

                // 중복된 뉴스 제목이 있는지 확인
                if (!realNewsRepository.existsByTitle(realNewsDto.title())) {
                    realNewsDtoList.add(realNewsDto);
                }

                //TODO : @Async, TaskScheduler 등으로 비동기 처리 고려
                Thread.sleep(crawlingDelay);
            }
            return saveAllRealNews(realNewsDtoList);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("런타임 인터럽트 발생");
        }
    }

    @Transactional
    public List<RealNewsDto> createRealNewsDtoByCrawl(List<NaverNewsDto> MetaDataList) {
        List<RealNewsDto> allRealNewsDtos = new ArrayList<>();

        try{
            for (NaverNewsDto metaData : MetaDataList) {
                Optional<NewsDetailDto> newsDetailData = crawladditionalInfo(metaData.link());

                if (newsDetailData.isEmpty()) {
                    // 크롤링 실패 시 해당 뉴스는 건너뜀
                    log.warn("크롤링 실패: {}", metaData.link());
                    continue;
                } else{
                    log.info("크롤링 성공: {}", metaData.link());
                }

                RealNewsDto realNewsDto = makeRealNewsFromInfo(metaData, newsDetailData.get());

                // 중복된 뉴스 제목이 있는지 확인
                if (!realNewsRepository.existsByTitle(realNewsDto.title())) {
                    allRealNewsDtos.add(realNewsDto);
                }
                Thread.sleep(crawlingDelay);
            }
            return allRealNewsDtos;
            //return 뉴스 필터하기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 인터럽트 상태 복원
            throw new ServiceException(500, "뉴스 크롤링이 중단되었습니다");
        }

    }

    @Transactional
    public List<RealNewsDto> saveAllRealNews(List<RealNewsDto> realNewsDtoList) {
            // DTO → Entity 변환 후 저장
            List<RealNews> realNewsList = realNewsMapper.toEntityList(realNewsDtoList);
            List<RealNews> savedEntities = realNewsRepository.saveAll(realNewsList); // 저장된 결과 받기

            // Entity → DTO 변환해서 반환
            return realNewsMapper.toDtoList(savedEntities);
    }

    // 네이버 API를 통해 메타데이터 수집
    @Transactional
    public List<NaverNewsDto> collectMetaDataFromNaver(List<String> keywords) {
        List<NaverNewsDto> allNews = new ArrayList<>();

        log.info("네이버 API 호출 시작: {} 개 키워드", keywords.size());

        for (String keyword : keywords) {
            try {
                List<NaverNewsDto> news = fetchNews(keyword);
                log.info("키워드 '{}': {}건 조회", keyword, news.size());

                // 네이버 뉴스만 필터링
                List<NaverNewsDto> naverOnlyNews = news.stream()
                        .filter(dto -> dto.link().contains("n.news.naver.com"))
                        .toList();

                allNews.addAll(naverOnlyNews);

                Thread.sleep(1000); // API 제한
            } catch (Exception e) {
                log.error("키워드 '{}' 조회 실패: {}", keyword, e.getMessage());
            }
        }

        log.info("네이버 API 호출 완료: 총 {}건", allNews.size());
        return allNews;
    }

    //N건 패치
    //예상되는 문제 : 최신순으로 하면 겹치는 부분이 많을 것 같음
    //주요기사를 받아오는 로직이 필요할 것 같은데... 쉽지않을듯 자체적으론 힘들듯하고 우선 ai 프롬프트를 잘 만져봐야할 것 같습니다.
    public List<NaverNewsDto> fetchNews(String keyword) {
        try {
            //display는 한 번에 보여줄 뉴스의 개수, sort는 정렬 기준 (date: 최신순, sim: 정확도순)
            //일단 3건 패치하도록 해놨습니다. yml 에서 작성해서 사용하세요(10건 이상 x)

            String url = naverUrl + keyword + "&display=" + newsDisplayCount + "&sort=" + newsSortOrder;

            // http 요청 헤더 설정 (아래는 네이버 디폴트 형식)
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", clientId);
            headers.set("X-Naver-Client-Secret", clientSecret);

            // http 요청 엔티티(헤더+바디) 생성
            // get이라 본문은 없고 헤더만 포함 -> 아래에서 string = null로 설정
            HttpEntity<String> entity = new HttpEntity<>(headers);

            //http 요청 수행
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class, keyword);

            if (response.getStatusCode() == HttpStatus.OK) {
                // JsonNode: json 구조를 트리 형태로 표현. json의 중첩 구조를 탐색할 때 사용
                // readTree(): json 문자열을 JsonNode 트리로 변환
                ObjectMapper mapper = new ObjectMapper();
                JsonNode items = mapper.readTree(response.getBody()).get("items");

                if (items != null) {
                    return getNewsMetaDataFromNaverApi(items);
                }
                return new ArrayList<>();
            }
            throw new RuntimeException("네이버 API 요청 실패");

        }
        catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 파싱 실패");
        }

    }

    //현재 문제. 간격 배치. 하나의 키워드만 되도록

    // 단건 크롤링
    public Optional<NewsDetailDto> crawladditionalInfo(String naverNewsUrl) {
        try{
            Document doc = Jsoup.connect(naverNewsUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")  // 브라우저인 척
                    .get();  // GET 요청으로 HTML 가져오기 (robots.txt에 걸리지 않도록)

            String content = Optional.ofNullable(doc.selectFirst("article#dic_area"))
                    .map(Element::text)
                    .orElse("");
            String imgUrl = Optional.ofNullable(doc.selectFirst("#img1"))
                    .map(element -> element.attr("data-src"))
                    .orElse("");

            String journalist = Optional.ofNullable(doc.selectFirst("em.media_end_head_journalist_name"))
                    .map(Element::text)
                    .orElse("");
            String mediaName = Optional.ofNullable(doc.selectFirst("img.media_end_head_top_logo_img"))
                    .map(elem -> elem.attr("alt"))
                    .orElse("");

            // 크롤링한 정보가 비어있으면 null 반환
            if(content.isEmpty() || imgUrl.isEmpty() || journalist.isEmpty() || mediaName.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(NewsDetailDto.of(content, imgUrl, journalist, mediaName));

        } catch (IOException e) {
            log.warn("크롤링 실패: {}", naverNewsUrl);
            return Optional.empty();  // 예외 던지지 않고 빈 값 반환
        }
    }

    // 네이버 api에서 받아온 정보와 크롤링한 상세 정보를 바탕으로 RealNewsDto 생성
    public RealNewsDto makeRealNewsFromInfo(NaverNewsDto naverNewsDto, NewsDetailDto newsDetailDto) {
        return RealNewsDto.of(
                null, // ID는 null로 시작, 저장 시 자동 생성
                naverNewsDto.title(),
                newsDetailDto.content(),
                naverNewsDto.description(),
                naverNewsDto.link(),
                newsDetailDto.imgUrl(),
                parseNaverDate(naverNewsDto.pubDate()),
                newsDetailDto.mediaName(),
                newsDetailDto.journalist(),
                naverNewsDto.originallink(),
                NewsCategory.NOT_FILTERED

        );
    }

    // fetchNews 메서드로 네이버 API에서 뉴스 목록을 가져오고
    // 링크 정보를 바탕으로 상세 정보를 crawlAddtionalInfo 메서드로 크롤링하여 RealNews 객체를 생성
    private List<NaverNewsDto> getNewsMetaDataFromNaverApi(JsonNode items){
        List<NaverNewsDto> newsMetaDataList = new ArrayList<>();

        for (JsonNode item : items) {
            String rawTitle = item.get("title").asText("");
            String originallink = item.get("originallink").asText("");
            String link = item.get("link").asText("");
            String rawDdscription = item.get("description").asText("");
            String pubDate = item.get("pubDate").asText("");

            String cleanedTitle = HtmlEntityDecoder.decode(rawTitle); // HTML 태그 제거
            String cleanDescription = HtmlEntityDecoder.decode(rawDdscription); // HTML 태그 제거

            //한 필드라도 비어있으면 건너뜀
            if(cleanedTitle.isEmpty()|| originallink.isEmpty() || link.isEmpty() || cleanDescription.isEmpty() || pubDate.isEmpty())
                continue;
            //팩토리 메서드 사용
            NaverNewsDto newsDto = NaverNewsDto.of(cleanedTitle, originallink, link, cleanDescription, pubDate);
            newsMetaDataList.add(newsDto);
        }

        return newsMetaDataList;
    }

    // 네이버 API에서 받아온 날짜 문자열을 LocalDateTime으로 변환
    private LocalDateTime parseNaverDate(String naverDate) {
        try {
            String cleaned = HtmlEntityDecoder.decode(naverDate);

            // 네이버 API 형식: "Tue, 29 Jul 2025 18:48:00 +0900"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

            // ZonedDateTime으로 파싱 후 LocalDateTime으로 변환 (시간대 정보 제거)
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(cleaned, formatter);
            return zonedDateTime.toLocalDateTime();

        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}. 현재 시간으로 설정", naverDate);
            return LocalDateTime.now();
        }
    }

    @Transactional
    public boolean deleteRealNews(Long newsId) {
        Optional<RealNews> realNewsOpt = realNewsRepository.findById(newsId);

        if (realNewsOpt.isEmpty()) {
            return false;  // 뉴스가 없으면 false 반환
        }

        if (todayNewsRepository.existsById(newsId)) {
            todayNewsRepository.deleteById(newsId);
        }
        // 뉴스 삭제 (FakeNews도 CASCADE로 함께 삭제됨)
        realNewsRepository.deleteById(newsId);
        return true;
    }


    public boolean isAlreadyTodayNews(Long id) {
        return todayNewsRepository.existsById(id);
    }

    @Transactional
    public void setTodayNews(Long id) {
        RealNews realNews = realNewsRepository.findById(id).
                orElseThrow(() -> new IllegalArgumentException("해당 ID의 뉴스가 존재하지 않습니다. ID: " + id));

        LocalDate today = LocalDate.now();
        todayNewsRepository.deleteBySelectedDate(today);

        // 5. 새로운 오늘의 뉴스 생성
        TodayNews todayNews = TodayNews.builder()
                .selectedDate(today)
                .realNews(realNews)
                .build();

        todayNewsRepository.save(todayNews);
    }

    public int count() {
        return (int) realNewsRepository.count();
    }

    public List<RealNewsDto> selectNewsByScore(List<AnalyzedNewsDto> allRealNewsAfterFilter) {
        return allRealNewsAfterFilter.stream()
                .collect(Collectors.groupingBy(AnalyzedNewsDto::category))
                .values()
                .stream()
                .flatMap(categoryNews ->
                        categoryNews.stream()
                                .sorted(Comparator.comparing(AnalyzedNewsDto::score).reversed())
                                .limit(3)
                )
                .map(AnalyzedNewsDto::realNewsDto)  // RealNewsDto로 변환
                .toList();
    }
}
