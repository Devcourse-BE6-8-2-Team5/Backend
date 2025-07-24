package com.back.domain.news.realNews.service;

import com.back.domain.news.realNews.dto.NaverNewsDto;
import com.back.domain.news.realNews.repository.RealNewsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

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

    public List<NaverNewsDto> fetchNews(String query) throws JsonProcessingException {

        //display는 한 번에 보여줄 뉴스의 개수, sort는 정렬 기준 (date: 최신순, sim: 정확도순)
        String url = "https://openapi.naver.com/v1/search/news?query={query}&display=10&sort=sim";

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

            // JsonNode: json 구조를 트리 형태로 표현하는 객체
            // json의 중첩 구조를 탐색할 때 사용 (예: response.items[0].title)
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
}
