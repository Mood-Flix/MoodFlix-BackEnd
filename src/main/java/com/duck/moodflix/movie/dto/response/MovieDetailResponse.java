package com.duck.moodflix.movie.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "영화 상세 응답 (상세 페이지용)")
public record MovieDetailResponse(
        Long id,
        Long tmdbId,
        String title,
        String originalTitle,
        String status,
        String overview,
        String posterUrl,
        LocalDate releaseDate,
        Integer runtime,
        List<String> genres,
        List<String> keywords,
        String countryCode,
        String countryName,
        String certification, // KR 등급
        Double voteAverage,
        Integer voteCount,
        Double popularity,
        Long budget,
        Long revenue,
        // 미디어/인물/연관작
        List<VideoItem> videos,
        List<ImageItem> posters,
        List<ImageItem> backdrops,
        List<CastItem> castTop,
        List<CrewItem> crewCore,
        List<RelatedItem> similar,
        List<RelatedItem> recommendations,
        // 리뷰 (상위 N개만)
        List<ReviewItem> reviewsTop
) {
    @Schema(description="비디오(YouTube 등)") public record VideoItem(String name, String key, String site, String type) {}
    @Schema(description="이미지")            public record ImageItem(String url, Integer width, Integer height, Double aspectRatio) {}
    @Schema(description="출연")              public record CastItem(String name, String character, String profileUrl, Integer order) {}
    @Schema(description="제작")              public record CrewItem(String name, String job, String profileUrl) {}
    @Schema(description="연관작")            public record RelatedItem(Long tmdbId, String title, String posterUrl, java.time.LocalDate releaseDate) {}
    @Schema(description="리뷰 요약")          public record ReviewItem(
            String author, Double rating, String content, String createdAt, String url, String avatarUrl
    ) {}
}
