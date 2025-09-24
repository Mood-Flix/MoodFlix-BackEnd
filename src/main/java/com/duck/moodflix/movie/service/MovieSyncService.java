package com.duck.moodflix.movie.service;

import com.duck.moodflix.movie.client.TMDbClient;
import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.dto.tmdb.TMDbMovieDetailDto;
import com.duck.moodflix.movie.dto.tmdb.TMDbMovieListResponse;
import com.duck.moodflix.movie.dto.tmdb.related.MovieBriefDto;
import com.duck.moodflix.movie.mapper.MovieMapper;
import com.duck.moodflix.movie.repository.MovieRepository;
import com.duck.moodflix.movie.util.AgeRatingDecider;
import com.duck.moodflix.movie.util.CertificationExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    private final CertificationExtractor certExtractor;
    private final MovieIndexService movieIndexService; // ES 색인 서비스

    /** 인기 영화 모든 페이지 동기화(한글 제목 지원 + 성인/등급/예산/흥행/개요 필터) */
    public int syncAllPopular() {
        int page = 1;
        int savedTotal = 0;

        while (true) {
            TMDbMovieListResponse resp = tmdb.getPopular(page);
            List<MovieBriefDto> briefs = (resp == null || resp.getResults() == null)
                    ? List.of() : resp.getResults();
            if (briefs.isEmpty()) {
                log.info("No more results. page={}", page);
                break;
            }

            int savedPage = 0, skipExist = 0, skipAdult = 0, skipNoKo = 0,
                    skipNoOverview = 0, skipMetaMissing = 0, skipError = 0;

            // 이번 페이지에서 DB에 저장된 영화들을 모아두기 → 벌크 색인
            List<Movie> savedThisPage = new ArrayList<>();

            for (MovieBriefDto brief : briefs) {
                Long tmdbId = brief.id();
                if (tmdbId == null) { skipError++; continue; }

                try {
                    // 1) 중복 스킵
                    if (movieRepository.existsByTmdbId(tmdbId)) { skipExist++; continue; }

                    // 2) 상세 조회(ko-KR)
                    TMDbMovieDetailDto d = tmdb.getMovieDetail(tmdbId);
                    if (d == null) { skipError++; continue; }

                    // 3) 성인물 필터 (TMDb 성인 플래그 + 등급 기반)
                    boolean adult = Boolean.TRUE.equals(d.getAdult());
                    String cert = null;
                    if (!adult) {
                        cert = certExtractor.extract(d);
                        if (AgeRatingDecider.isAdultCert(cert)) adult = true;
                    }
                    if (adult) { skipAdult++; continue; }

                    // 4) 한글 번역 여부 필터
                    if (!tmdb.hasKoreanTranslation(tmdbId)) { skipNoKo++; continue; }

                    // 5) overview (ko → en 폴백) 필수
                    String ko = d.getOverview();
                    String en = null;
                    if (ko == null || ko.isBlank()) {
                        var enD = tmdb.getMovieDetail(tmdbId, "en-US");
                        if (enD != null && enD.getOverview() != null && !enD.getOverview().isBlank()) {
                            en = enD.getOverview();
                        }
                    }
                    if ((ko == null || ko.isBlank()) && (en == null || en.isBlank())) {
                        skipNoOverview++; continue;
                    }

                    // 6) 메타데이터(등급/예산/흥행) 필수
                    boolean missing =
                            (cert == null || cert.isBlank()) ||
                                    (d.getBudget() == null || d.getBudget() == 0) ||
                                    (d.getRevenue() == null || d.getRevenue() == 0);
                    if (missing) { skipMetaMissing++; continue; }

                    // 7) 저장
                    Movie movie = movieMapper.toEntity(d, en);
                    movieRepository.save(movie);

                    // 8) 키워드/리뷰 후처리
                    var keywordNames = (d.getKeywords()==null || d.getKeywords().keywords()==null)
                            ? List.<String>of()
                            : d.getKeywords().keywords().stream()
                            .map(k -> k.name()).filter(Objects::nonNull)
                            .map(String::trim).filter(s -> !s.isEmpty())
                            .distinct().toList();
                    keywordManager.upsert(movie, keywordNames);
                    reviewSyncService.syncForMovie(movie);

                    savedThisPage.add(movie);
                    savedPage++;

                } catch (Exception e) {
                    skipError++;
                    log.warn("Skip by exception. tmdbId={}, msg={}", tmdbId, e.getMessage());
                }
            }

            // 9) 페이지 끝에서 ES 벌크 색인
            if (!savedThisPage.isEmpty()) {
                try {
                    movieIndexService.indexMovies(savedThisPage);
                    log.info("indexed to ES: page={}, count={}", page, savedThisPage.size());
                } catch (Exception ex) {
                    log.error("ES indexing failed. page={}, err={}", page, ex.toString());
                }
            }

            log.info("popular page={} result: saved={}, exist={}, adult={}, noKo={}, noOverview={}, metaMissing={}, error={}",
                    page, savedPage, skipExist, skipAdult, skipNoKo, skipNoOverview, skipMetaMissing, skipError);
            savedTotal += savedPage;

            Integer totalPages = resp.getTotalPages();
            int last = (totalPages == null || totalPages <= 0) ? 500 : Math.min(totalPages, 500);
            if (page >= last) {
                log.info("Reached last page={} (tmdb total_pages={}, cap=500)", page, totalPages);
                break;
            }
            page++;
        }

        log.info("Sync all popular completed. saved={}", savedTotal);
        return savedTotal;
    }

    /** 연도 범위로 Discover 동기화(+필터) */
    public int syncDiscoverByYearRange(int fromYear, int toYear) {
        int savedTotal = 0;
        int start = Math.min(fromYear, toYear);
        int end   = Math.max(fromYear, toYear);

        for (int year = start; year <= end; year++) {
            int page = 1;
            while (true) {
                var resp = tmdb.discoverByYear(year, page); // 성인 제외
                var briefs = (resp == null || resp.getResults() == null) ? List.<MovieBriefDto>of() : resp.getResults();
                if (briefs.isEmpty()) {
                    log.info("discover year={} page={} empty, stop.", year, page);
                    break;
                }

                int savedPage = 0, skipExist=0, skipAdult=0, skipNoKo=0, skipNoOverview=0, skipMetaMissing=0, skipErr=0;

                // 이번 페이지에서 저장된 영화 모아서 색인
                List<Movie> savedThisPage = new ArrayList<>();

                for (var brief : briefs) {
                    Long tmdbId = brief.id();
                    if (tmdbId == null) { skipErr++; continue; }

                    try {
                        if (movieRepository.existsByTmdbId(tmdbId)) { skipExist++; continue; }

                        TMDbMovieDetailDto d = tmdb.getMovieDetail(tmdbId); // ko-KR
                        if (d == null) { skipErr++; continue; }

                        boolean adult = Boolean.TRUE.equals(d.getAdult());
                        String cert = null;
                        if (!adult) {
                            cert = certExtractor.extract(d);
                            if (AgeRatingDecider.isAdultCert(cert)) adult = true;
                        }
                        if (adult) { skipAdult++; continue; }

                        if (!tmdb.hasKoreanTranslation(tmdbId)) { skipNoKo++; continue; }

                        String ko = d.getOverview();
                        String en = null;
                        if (ko == null || ko.isBlank()) {
                            var dEn = tmdb.getMovieDetail(tmdbId, "en-US");
                            if (dEn != null && dEn.getOverview()!=null && !dEn.getOverview().isBlank()) en = dEn.getOverview();
                        }
                        if ((ko == null || ko.isBlank()) && (en == null || en.isBlank())) { skipNoOverview++; continue; }

                        boolean missing = (cert == null || cert.isBlank())
                                || (d.getBudget()==null || d.getBudget()==0)
                                || (d.getRevenue()==null || d.getRevenue()==0);
                        if (missing) { skipMetaMissing++; continue; }

                        Movie movie = movieMapper.toEntity(d, en);
                        movieRepository.save(movie);

                        var keywordNames = (d.getKeywords()==null || d.getKeywords().keywords()==null)
                                ? List.<String>of()
                                : d.getKeywords().keywords().stream()
                                .map(k -> k.name()).filter(Objects::nonNull)
                                .map(String::trim).filter(s -> !s.isEmpty())
                                .distinct().toList();
                        keywordManager.upsert(movie, keywordNames);

                        reviewSyncService.syncForMovie(movie);

                        savedThisPage.add(movie);
                        savedPage++;

                    } catch (Exception e) {
                        skipErr++;
                        log.warn("discover sync skip by exception. tmdbId={}, msg={}", tmdbId, e.getMessage());
                    }
                }

                // 페이지 끝에서 ES 벌크 색인
                if (!savedThisPage.isEmpty()) {
                    try {
                        movieIndexService.indexMovies(savedThisPage);
                        log.info("indexed to ES (discover): year={}, page={}, count={}", year, page, savedThisPage.size());
                    } catch (Exception ex) {
                        log.error("ES indexing failed (discover). year={}, page={}, err={}", year, page, ex.toString());
                    }
                }

                log.info("discover year={} page={} result: saved={}, exist={}, adult={}, noKo={}, noOverview={}, metaMissing={}, error={}",
                        year, page, savedPage, skipExist, skipAdult, skipNoKo, skipNoOverview, skipMetaMissing, skipErr);
                savedTotal += savedPage;

                Integer totalPages = resp.getTotalPages();
                int last = (totalPages == null || totalPages <= 0) ? 500 : Math.min(totalPages, 500);
                if (page >= last) break;
                page++;
            }
        }
        log.info("discover sync completed. years {}~{}, saved={}", start, end, savedTotal);
        return savedTotal;
    }
}
