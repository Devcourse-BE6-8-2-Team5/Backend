package com.back.domain.news.real.mapper;

import com.back.domain.news.real.dto.RealNewsDto;
import com.back.domain.news.real.entity.RealNews;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RealNewsMapper {
    public List<RealNews> toEntityList(List<RealNewsDto> realNewsDtoList) {
        return realNewsDtoList.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public RealNews toEntity(RealNewsDto realNewsDto) {
        return new RealNews(
                realNewsDto.title(),
                realNewsDto.content(),
                realNewsDto.description(),
                realNewsDto.link(),
                realNewsDto.imgUrl(),
                realNewsDto.originCreatedDate(),
                realNewsDto.mediaName(),
                realNewsDto.journalist(),
                realNewsDto.originalNewsUrl(),
                realNewsDto.newsCategory()
        );
    }

    public List<RealNewsDto> toDtoList(List<RealNews> realNewsList) {
        return realNewsList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public RealNewsDto toDto(RealNews realNews) {
        return RealNewsDto.of(
                realNews.getTitle(),
                realNews.getContent(),
                realNews.getDescription(),
                realNews.getLink(),
                realNews.getImgUrl(),
                realNews.getOriginCreatedDate(),
                realNews.getMediaName(),
                realNews.getJournalist(),
                realNews.getOriginalNewsUrl(),
                realNews.getNewsCategory()
        );
    }


}
