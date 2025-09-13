package com.duck.moodflix.movie.mapper;

import com.duck.moodflix.auth.config.TmdbProperties;
import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.dto.tmdb.TMDbMovieDetailDto;
import com.duck.moodflix.movie.util.AgeRatingDecider;
import com.duck.moodflix.movie.util.CertificationExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class MovieMapper {

    private final TmdbProperties props;
    private final CertificationExtractor certExtractor;

    /** 기존 한-인자 호출을 위해 오버로드 추가 */
    public Movie toEntity(TMDbMovieDetailDto d) {
        return toEntity(d, null);
    }

    /** overviewFallback이 있으면 d.getOverview()가 비어있을 때 사용 */
    public Movie toEntity(TMDbMovieDetailDto d, String overviewFallback) {
        if (d == null) throw new IllegalArgumentException("TMDbMovieDetailDto must not be null");

        String poster = (d.getPosterPath()==null || d.getPosterPath().isBlank())
                ? null : props.getPosterBaseUrl() + d.getPosterPath();

        String genre = (d.getGenres()==null || d.getGenres().isEmpty() || d.getGenres().get(0)==null)
                ? null : d.getGenres().get(0).name();

        LocalDate date = safe(d.getReleaseDate());

        // 성인 여부(기존 로직 유지)
        boolean adult = Boolean.TRUE.equals(d.getAdult());
        if (!adult) {
            String cert = certExtractor.extract(d);
            adult = AgeRatingDecider.isAdultCert(cert);
        }

        // ★ overview 폴백 적용
        String overview = firstNonBlank(d.getOverview(), overviewFallback, null);

        return Movie.builder()
                .tmdbId(d.getId())
                .title(d.getTitle())
                .overview(overview)
                .posterUrl(poster)
                .releaseDate(date)
                .genre(genre)
                .voteAverage(d.getVoteAverage())
                .adult(adult)
                .build();
    }

    private static String firstNonBlank(String... arr) {
        if (arr == null) return null;
        for (String s : arr) if (s != null && !s.isBlank()) return s;
        return null;
    }

    private LocalDate safe(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }
}

