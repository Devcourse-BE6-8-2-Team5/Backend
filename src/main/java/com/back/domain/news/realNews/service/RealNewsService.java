package com.back.domain.news.realNews.service;

import com.back.domain.news.realNews.dto.NaverNewsDto;
import com.back.domain.news.realNews.dto.NewsDetailDto;
import com.back.domain.news.realNews.dto.RealNewsDto;
import com.back.domain.news.realNews.entity.RealNews;
import com.back.domain.news.realNews.repository.RealNewsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


@Service
@RequiredArgsConstructor
public class RealNewsService {
    private final RealNewsRepository realNewsRepository;

    // application.yml에서 인증정보 가져옴
    @Value("${NAVER_CLIENT_ID}")
    private String clientId;

    @Value("${NAVER_CLIENT_SECRET}")
    private String clientSecret;

    // HTTP 요청을 보내기 위한 Spring의 HTTP 클라이언트(외부 API 호출 시 사용)
    private final RestTemplate restTemplate = new RestTemplate();


    //다건 패치
    public List<NaverNewsDto> fetchNews(String query) throws JsonProcessingException {

        //display는 한 번에 보여줄 뉴스의 개수, sort는 정렬 기준 (date: 최신순, sim: 정확도순)
        //일단 3건 패치하도록 해놨습니다.
        String url = "https://openapi.naver.com/v1/search/news?query={query}&display=3&sort=sim";

        //예상되는 문제 : 최신순으로 하면 겹치는 부분이 많을 것 같음 -> 최신순 display 늘려서 거기서 ai를 통한 필터링?
        //주요기사를 받아오는 로직이 필요할 것 같은데... 쉽지않을듯 자체적으론 힘들듯하고 우선 ai 프롬프트를 잘 만져봐야할 것 같다

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

            //json 파싱
            ObjectMapper mapper = new ObjectMapper();

            // JsonNode: json 구조를 트리 형태로 표현. json의 중첩 구조를 탐색할 때 사용
            // readTree(): json 문자열을 JsonNode 트리로 변환
            JsonNode items = mapper.readTree(response.getBody()).get("items");

            List<NaverNewsDto> newsList = new ArrayList<>();
            if (items != null) {
                for (JsonNode item : items) {
                    String title = item.get("title").asText("");
                    String originallink = item.get("originallink").asText("");
                    String link = item.get("link").asText("");
                    String description = item.get("description").asText("");
                    String pubDate = item.get("pubDate").asText("");

                    //팩토리 메서드 사용
                    NaverNewsDto newsDto = NaverNewsDto.of(title, originallink, link, description, pubDate);
                    newsList.add(newsDto);
                }
            }
            return newsList;
        }

        // 예외 처리: HTTP 요청 실패 시
        throw new RuntimeException("Failed to fetch news");
    }


    // 단건 크롤링
    // 크롤링을 단시간내에 많이 하면 네이버에서 IP를 차단할 수 있으므로 간격을 둬야함
    public NewsDetailDto crawlAddtionalInfo(String naverNewsUrl) throws IOException {
        try{
            Document doc = Jsoup.connect(naverNewsUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")  // 브라우저인 척
                    .get();  // GET 요청으로 HTML 가져오기 (robots.txt에 걸리지 않도록)

            String content = doc.selectFirst("article#dic_area").text();
            String imgUrl = doc.selectFirst("#img1").attr("data-src");
            String journalist = doc.selectFirst("em.media_end_head_journalist_name").text();
            String mediaName = doc.selectFirst("img.media_end_head_top_logo_img").attr("alt");

            System.out.println(doc.html());

            return NewsDetailDto.of(content, imgUrl, journalist, mediaName);
        } catch (IOException e) {
            //  Jsoup 연결 실패 시
            throw new IOException("크롤링 실패: " + naverNewsUrl, e);
        }
    }

    // 네이버 api에서 받아온 정보와 크롤링한 상세 정보를 바탕으로 RealNewsDto 생성
    public RealNewsDto buildRealNewsDtoEntity(NaverNewsDto naverNewsDto, NewsDetailDto newsDetailDto) {
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

    public List<RealNewsDto> createRealNews(String query) throws IOException {

        try{
            List<NaverNewsDto> naverNewsList = fetchNews(query);
            List<RealNewsDto> realNewsDtoList = new ArrayList<>();

            for(NaverNewsDto naverNews : naverNewsList) {
                NewsDetailDto newsDetailDto = crawlAddtionalInfo(naverNews.link());
                RealNewsDto realNewsdto = buildRealNewsDtoEntity(naverNews, newsDetailDto);
                realNewsDtoList.add(realNewsdto);

                Thread.sleep(500); // 0.5초 대기 일단 설정 해놓을게요 (네이버에서 IP 차단 방지)
            }

            List<RealNews> realNewsList = realNewsDtoList.stream()
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
            //DB 저장
            List<RealNews> savedEntities = realNewsRepository.saveAll(realNewsList);

            // Entity → DTO 변환해서 반환
            return savedEntities.stream()
                    .map(entity -> RealNewsDto.of(
                            entity.getTitle(),
                            entity.getContent(),
                            entity.getDescription(),
                            entity.getLink(),
                            entity.getImgUrl(),
                            entity.getOriginCreatedDate(),
                            entity.getMediaName(),
                            entity.getJournalist(),
                            entity.getOriginalNewsUrl()
                    ))
                    .toList();


        } catch(JsonProcessingException | InterruptedException e) {
            // 예외 처리: JSON 파싱 실패
            throw new RuntimeException("JSON 파싱 실패: " + e.getMessage(), e);
        }
    }

    // 네이버 API에서 받아온 날짜 문자열을 LocalDateTime으로 변환
    private LocalDateTime parseNaverDate(String naverDate) {
        try {
            String dateTimeFormat = naverDate.replaceAll("\\s[+-]\\d{4}$", "");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm", Locale.ENGLISH);
            return LocalDateTime.parse(dateTimeFormat , formatter);
        } catch (Exception e) {
            return LocalDateTime.now(); // 파싱 실패 시 현재 시간
        }
    }
}