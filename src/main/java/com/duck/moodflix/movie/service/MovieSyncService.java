package com.duck.moodflix.movie.service;

import com.duck.moodflix.movie.client.TMDbClient;
import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.dto.tmdb.TMDbMovieDetailDto;
import com.duck.moodflix.movie.dto.tmdb.TMDbMovieListResponse;
import com.duck.moodflix.movie.dto.tmdb.related.MovieBriefDto;
import com.duck.moodflix.movie.mapper.MovieMapper;
import com.duck.moodflix.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieSyncService {

    private final MovieRepository movieRepository;
    private final TMDbClient tmdb;
    private final MovieMapper movieMapper;
    private final KeywordManager keywordManager;
    private final ReviewSyncService reviewSyncService;
    private final TransactionTemplate tx;

    public void syncPopularPage1() {
        TMDbMovieListResponse resp = tmdb.getPopular(1);
        List<MovieBriefDto> briefs = (resp == null || resp.getResults() == null) ? List.of() : resp.getResults();
        if (briefs.isEmpty()) { log.warn("No popular results from TMDb"); return; }

        int saved = 0;
        for (MovieBriefDto brief : briefs) {
            Long tmdbId = brief.id();
            if (tmdbId == null || movieRepository.existsByTmdbId(tmdbId)) continue;

            TMDbMovieDetailDto detail = tmdb.getMovieDetail(tmdbId);
            if (detail == null) { log.warn("Skip detail null. tmdbId={}", tmdbId); continue; }

            Movie movie = tx.execute(status -> {
                Movie m = movieMapper.toEntity(detail);
                movieRepository.save(m);
                var names = (detail.getKeywords()==null || detail.getKeywords().keywords()==null)
                        ? List.<String>of()
                        : detail.getKeywords().keywords().stream()
                        .filter(Objects::nonNull).map(k -> k.name()).filter(Objects::nonNull).distinct().toList();
                keywordManager.upsert(m, names);
                return m;
            });

            reviewSyncService.syncForMovie(movie); // 트랜잭션 밖
            saved++;
        }
        log.info("Movie sync completed. saved={}", saved);
    }
}
