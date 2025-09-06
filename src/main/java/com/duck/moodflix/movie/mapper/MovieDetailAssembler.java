package com.duck.moodflix.movie.mapper;

import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.dto.response.MovieDetailResponse;
import com.duck.moodflix.movie.dto.tmdb.TMDbMovieDetailDto;
import com.duck.moodflix.movie.util.CertificationExtractor;
import com.duck.moodflix.movie.util.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
public class MovieDetailAssembler {

    private final ImageUrlResolver img;
    private final CertificationExtractor cert;

    public MovieDetailResponse assemble(Movie m, TMDbMovieDetailDto d) {

        List<String> genres = (d != null && d.getGenres() != null)
                ? d.getGenres().stream().map(g -> g.name()).filter(Objects::nonNull).toList()
                : List.of();

        List<String> keywords = m.getMovieKeywords().stream()
                .map(mk -> mk.getKeyword().getName())
                .filter(Objects::nonNull).distinct().toList();

        // 국가/등급
        String countryCode = null, countryName = null;
        if (d != null && d.getProductionCountries() != null && !d.getProductionCountries().isEmpty()
                && d.getProductionCountries().get(0) != null) {
            countryCode = d.getProductionCountries().get(0).iso31661();
            countryName = d.getProductionCountries().get(0).name();
        }
        String certification = (d != null) ? cert.extract(d) : null;

        // 비디오/이미지
        var videos = (d == null || d.getVideos() == null || d.getVideos().results() == null) ? List.<MovieDetailResponse.VideoItem>of()
                : d.getVideos().results().stream().limit(8)
                .map(v -> new MovieDetailResponse.VideoItem(v.name(), v.key(), v.site(), v.type()))
                .toList();

        var posters = (d == null || d.getImages() == null || d.getImages().posters() == null) ? List.<MovieDetailResponse.ImageItem>of()
                : d.getImages().posters().stream().limit(12)
                .map(i -> new MovieDetailResponse.ImageItem(
                        img.w500(i.filePath()), i.width(), i.height(), i.aspectRatio()))
                .toList();

        var backdrops = (d == null || d.getImages() == null || d.getImages().backdrops() == null) ? List.<MovieDetailResponse.ImageItem>of()
                : d.getImages().backdrops().stream().limit(12)
                .map(i -> new MovieDetailResponse.ImageItem(
                        img.w780(i.filePath()), i.width(), i.height(), i.aspectRatio()))
                .toList();

        // 출연/크루
        var castTop = (d == null || d.getCredits() == null || d.getCredits().cast() == null) ? List.<MovieDetailResponse.CastItem>of()
                : d.getCredits().cast().stream()
                .sorted(Comparator.comparingInt(c -> c.order() == null ? 999 : c.order()))
                .limit(10)
                .map(c -> new MovieDetailResponse.CastItem(
                        c.name(), c.character(), img.w185(c.profilePath()), c.order()))
                .toList();

        Set<String> core = Set.of("Director", "Writer", "Screenplay", "Producer", "Executive Producer",
                "Editor", "Cinematography", "Original Music Composer");
        var crewCore = (d == null || d.getCredits() == null || d.getCredits().crew() == null) ? List.<MovieDetailResponse.CrewItem>of()
                : d.getCredits().crew().stream()
                .filter(c -> c.job() != null && core.contains(c.job()))
                .limit(12)
                .map(c -> new MovieDetailResponse.CrewItem(
                        c.name(), c.job(), img.w185(c.profilePath())))
                .toList();

        // 연관작 (both need LocalDate)
        var similar = (d == null || d.getSimilar() == null || d.getSimilar().results() == null) ? List.<MovieDetailResponse.RelatedItem>of()
                : d.getSimilar().results().stream().limit(10)
                .map(r -> new MovieDetailResponse.RelatedItem(
                        r.id(), r.title(), img.w342(r.posterPath()), safeLocal(r.releaseDate())))
                .toList();

        var recs = (d == null || d.getRecommendations() == null || d.getRecommendations().results() == null) ? List.<MovieDetailResponse.RelatedItem>of()
                : d.getRecommendations().results().stream().limit(10)
                .map(r -> new MovieDetailResponse.RelatedItem(
                        r.id(), r.title(), img.w342(r.posterPath()), safeLocal(r.releaseDate())))
                .toList();

        return new MovieDetailResponse(
                // 기본
                m.getId(),
                m.getTmdbId(),
                coalesce(m.getTitle(), d == null ? null : d.getTitle()),
                d == null ? null : d.getOriginalTitle(),
                d == null ? null : d.getStatus(),
                coalesce(m.getOverview(), d == null ? null : d.getOverview()),
                coalesce(m.getPosterUrl(), d == null ? null : img.w500(d.getPosterPath())),
                d == null ? m.getReleaseDate() : safe(d.getReleaseDate()),
                d == null ? null : d.getRuntime(),

                // 분류/태그
                genres,
                keywords,

                // 국가/등급
                countryCode,
                countryName,
                certification,

                // 지표/통계
                coalesce(m.getVoteAverage(), d == null ? null : d.getVoteAverage()),
                d == null ? null : d.getVoteCount(),
                d == null ? null : d.getPopularity(),
                d == null ? null : d.getBudget(),
                d == null ? null : d.getRevenue(),

                // 미디어/인물/연관 (리뷰는 QueryService에서 폴백/합성)
                videos,
                posters,
                backdrops,
                castTop,
                crewCore,
                similar,
                recs,
                List.of() // reviewsTop
        );
    }

    private static LocalDate safeLocal(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }

    private LocalDate safe(String s) {
        try { return s == null || s.isBlank() ? null : LocalDate.parse(s); }
        catch (Exception e) { return null; }
    }

    private <T> T coalesce(T a, T b) { return a != null ? a : b; }
}
