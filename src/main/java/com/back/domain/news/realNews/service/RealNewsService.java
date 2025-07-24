package com.back.domain.news.realNews.service;

import com.back.domain.news.realNews.dto.NaverNewsDto;
import com.back.domain.news.realNews.dto.NewsDetailDto;
import com.back.domain.news.realNews.dto.RealNewsDto;
import com.back.domain.news.realNews.entity.RealNews;
import com.back.domain.news.realNews.repository.RealNewsRepository;
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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
@RequiredArgsConstructor
public class RealNewsService {
    private final RealNewsRepository realNewsRepository;
    // HTTP 요청을 보내기 위한 Spring의 HTTP 클라이언트(외부 API 호출 시 사용)
    private final RestTemplate restTemplate = new RestTemplate();

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

    // RealNewsDto를 생성하는 메서드
    public List<RealNewsDto> createRealNews(String query) {

        try{
            List<NaverNewsDto> naverMetaDataList = fetchNews(query);
            List<RealNewsDto> realNewsDtoList = new ArrayList<>();

            for(NaverNewsDto naverMetaData : naverMetaDataList) {
                NewsDetailDto newsDetailData = crawlAddtionalInfo(naverMetaData.link());
                RealNewsDto realNewsDto = MakeRealNewsFromInf(naverMetaData, newsDetailData);
                realNewsDtoList.add(realNewsDto);

                Thread.sleep(crawlingDelay);
            }
            // DTO → Entity 변환 후 저장
            List<RealNews> realNewsList = convertRealNewsDtoToEntity(realNewsDtoList);
            List<RealNews> savedEntities = realNewsRepository.saveAll(realNewsList); // 저장된 결과 받기

            // Entity → DTO 변환해서 반환
            return convertRealNewsEntityToDto(savedEntities);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("스레드 인터럽트 발생" );
        }
    }

    //N건 패치
    //예상되는 문제 : 최신순으로 하면 겹치는 부분이 많을 것 같음
    //주요기사를 받아오는 로직이 필요할 것 같은데... 쉽지않을듯 자체적으론 힘들듯하고 우선 ai 프롬프트를 잘 만져봐야할 것 같습니다.
    public List<NaverNewsDto> fetchNews(String query) {

        try {
            //display는 한 번에 보여줄 뉴스의 개수, sort는 정렬 기준 (date: 최신순, sim: 정확도순)
            //일단 3건 패치하도록 해놨습니다. yml 에서 작성해서 사용하세요(10건 이상 x)
            String url = "https://openapi.naver.com/v1/search/news?query={query}&display=" + newsDisplayCount + "&sort=" + newsSortOrder;

            // http 요청 헤더 설정 (아래는 네이버 디폴트 형식)
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", clientId);
            headers.set("X-Naver-Client-Secret", clientSecret);

            // http 요청 엔티티(헤더+바디) 생성
            // get이라 본문은 없고 헤더만 포함 -> 아래에서 string = null로 설정
            HttpEntity<String> entity = new HttpEntity<>(headers);

            //http 요청 수행
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class, query);

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

    // 단건 크롤링
    public NewsDetailDto crawlAddtionalInfo(String naverNewsUrl) {
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
//
//            if(content.isEmpty() || imgUrl.isEmpty() || journalist.isEmpty() || mediaName.isEmpty()) {
//                return null;
//            }

            return NewsDetailDto.of(content, imgUrl, journalist, mediaName);

        } catch (IOException e) {
            //  Jsoup 연결 실패 시
            throw new RuntimeException("크롤링 실패");
        }
    }

    // 네이버 api에서 받아온 정보와 크롤링한 상세 정보를 바탕으로 RealNewsDto 생성
    public RealNewsDto MakeRealNewsFromInf(NaverNewsDto naverNewsDto, NewsDetailDto newsDetailDto) {
        return RealNewsDto.of(
                naverNewsDto.title(),
                newsDetailDto.content(),
                naverNewsDto.description(),
                naverNewsDto.link(),
                newsDetailDto.imgUrl(),
                parseNaverDate(naverNewsDto.pubDate()),
                newsDetailDto.mediaName(),
                newsDetailDto.journalist(),
                naverNewsDto.originallink()
        );
    }
    // 뉴스 생성 메서드
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
            String dateTimeFormat = HtmlEntityDecoder.decode(naverDate);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm", Locale.ENGLISH);
            return LocalDateTime.parse(dateTimeFormat , formatter);
        } catch (Exception e) {
            return LocalDateTime.now(); // 파싱 실패 시 현재 시간
        }
    }

    private List<RealNews> convertRealNewsDtoToEntity(List<RealNewsDto> realNewsDtoList) {
        return realNewsDtoList.stream()
                .map(realNewsDto -> new RealNews(
                        realNewsDto.title(),
                        realNewsDto.content(),
                        realNewsDto.description(),
                        realNewsDto.link(),
                        realNewsDto.imgUrl(),
                        realNewsDto.originCreatedDate(),
                        realNewsDto.mediaName(),
                        realNewsDto.journalist(),
                        realNewsDto.originalNewsUrl()
                )).toList();
    }

    private List<RealNewsDto> convertRealNewsEntityToDto(List<RealNews> realNewsList) {
        return realNewsList.stream()
                .map(realNews -> RealNewsDto.of(
                        realNews.getTitle(),
                        realNews.getContent(),
                        realNews.getDescription(),
                        realNews.getLink(),
                        realNews.getImgUrl(),
                        realNews.getOriginCreatedDate(),
                        realNews.getMediaName(),
                        realNews.getJournalist(),
                        realNews.getOriginalNewsUrl()
                )).toList();
    }

}