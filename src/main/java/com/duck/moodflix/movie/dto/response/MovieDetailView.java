package com.duck.moodflix.movie.dto.response;


import java.util.List;


import java.time.LocalDate;


public record MovieDetailView(
        // 기본
        Long id,
        Long tmdbId,
        String title,
        String overview,
        String posterUrl,
        String genre,
        Double voteAverage,

        // 태그
        List<String> keywords,

        // ====== 추가 필드들 ======
        String originalTitle,
        String status,
        LocalDate releaseDate,
        Integer runtime,
        String countryCode,
        String countryName,
        String certification,   // KR 관람등급
        Integer voteCount,
        Double popularity,
        Long budget,
        Long revenue,

        // 인물/미디어/연관/리뷰
        List<Person> directors,
        List<Cast> castTop,
        List<Video> videos,
        List<Image> posters,
        List<Image> backdrops,
        List<Related> similar,
        List<Related> recommendations,
        List<Review> reviewsTop
) {
    public record Person(String name) {}
    public record Cast(String name, String character) {}
    public record Video(String name, String key, String site, String type) {}
    public record Image(String url, Integer width, Integer height, Double aspectRatio) {}
    public record Related(Long tmdbId, String title, String posterUrl, String releaseDate) {}
    public record Review(String author, Double rating, String content, String createdAt) {}
}