package com.duck.moodflix.movie.mapper;

import com.duck.moodflix.auth.config.TMDbProperties;
import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.dto.tmdb.TMDbMovieDetailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class MovieMapper {

    private final TMDbProperties props;

    public Movie toEntity(TMDbMovieDetailDto d) {
        String poster = (d.getPosterPath()==null || d.getPosterPath().isBlank())
                ? null : props.getPosterBaseUrl() + d.getPosterPath();

        String genre = (d.getGenres()==null || d.getGenres().isEmpty() || d.getGenres().get(0)==null)
                ? null : d.getGenres().get(0).name();

        LocalDate date = safe(d.getReleaseDate());

        return Movie.builder()
                .tmdbId(d.getId())
                .title(d.getTitle())
                .overview(d.getOverview())
                .posterUrl(poster)
                .releaseDate(date)
                .genre(genre)
                .voteAverage(d.getVoteAverage())
                .build();
    }

    private LocalDate safe(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }
}
