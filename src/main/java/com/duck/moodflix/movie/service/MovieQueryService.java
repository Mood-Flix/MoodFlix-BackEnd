package com.duck.moodflix.movie.service;

import com.duck.moodflix.movie.client.TMDbClient;
import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.dto.response.MovieDetailResponse;
import com.duck.moodflix.movie.dto.response.MovieSummaryResponse;
import com.duck.moodflix.movie.dto.tmdb.reviews.ReviewsPageDto;
import com.duck.moodflix.movie.mapper.MovieDetailAssembler;
import com.duck.moodflix.movie.repository.MovieRepository;
import com.duck.moodflix.movie.repository.TmdbReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieQueryService {

    private final MovieRepository movieRepository;
    private final TMDbClient tmdbClient;                 // TMDb 상세/리뷰 호출
    private final MovieDetailAssembler detailAssembler;  // 상세 응답 조립
    private final TmdbReviewRepository reviewRepo;       // 리뷰 폴백

    /** ✅ 페이징 요약 조회 */
    @Transactional(readOnly = true)
    public Page<MovieSummaryResponse> getMovieSummaries(Pageable pageable, boolean includeAdult) {
        Page<Movie> pg = includeAdult
                ? movieRepository.findAll(pageable)
                : movieRepository.findByAdultFalse(pageable);

        return pg.map(this::toSummary);
    }

    private MovieSummaryResponse toSummary(Movie m) {
        return new MovieSummaryResponse(
                m.getId(),
                m.getTmdbId(),
                m.getTitle(),
                m.getPosterUrl(),
                m.getGenre(),
                m.getReleaseDate(),
                m.getVoteAverage()
        );
    }

    /** 상세: ko-KR(append) → en-US(API) → DB 저장분 순 폴백으로 reviewsTop 보강 */
    @Transactional(readOnly = true)
    public MovieDetailResponse getMovieDetailResponse(Long id) {
        Movie m = movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("영화를 찾을 수 없습니다. ID: " + id));

        // 1) TMDb ko-KR 상세 + 기본 조립
        var detail = tmdbClient.getMovieDetail(m.getTmdbId());
        var resp = detailAssembler.assemble(m, detail);

        // 2) ko-KR 리뷰가 이미 있으면 그대로 반환
        if (resp.reviewsTop() != null && !resp.reviewsTop().isEmpty()) {
            return resp;
        }

        // 3) en-US 리뷰 폴백
        final String IMG_ROOT = "https://image.tmdb.org/t/p";
        ReviewsPageDto enPage = tmdbClient.getReviews(m.getTmdbId(), "en-US", 1);
        if (enPage != null && enPage.results() != null && !enPage.results().isEmpty()) {
            var enTop = enPage.results().stream()
                    .limit(5)
                    .map(r -> new MovieDetailResponse.ReviewItem(
                            r.author(),
                            r.authorDetails() == null ? null : r.authorDetails().rating(),
                            r.content(),
                            r.createdAt(),
                            r.url(),
                            buildAvatarUrl(IMG_ROOT, r.authorDetails() == null ? null : r.authorDetails().avatarPath())
                    ))
                    .toList();

            return new MovieDetailResponse(
                    resp.id(), resp.tmdbId(), resp.title(), resp.originalTitle(), resp.status(),
                    resp.overview(), resp.posterUrl(), resp.releaseDate(), resp.runtime(),
                    resp.genres(), resp.keywords(), resp.countryCode(), resp.countryName(),
                    resp.certification(), resp.voteAverage(), resp.voteCount(), resp.popularity(),
                    resp.budget(), resp.revenue(), resp.videos(), resp.posters(), resp.backdrops(),
                    resp.castTop(), resp.crewCore(), resp.similar(), resp.recommendations(), enTop
            );
        }

        // 4) DB 저장분 폴백 (createdAtTmdb 내림차순 5개)
        var stored = reviewRepo.findTop5ByMovie_IdOrderByCreatedAtTmdbDesc(m.getId());
        var storedMapped = stored.stream()
                .map(s -> new MovieDetailResponse.ReviewItem(
                        s.getAuthor(),
                        s.getRating(),
                        s.getContent(),
                        s.getCreatedAtTmdb() == null ? null : s.getCreatedAtTmdb().toString(),
                        s.getUrl(),
                        null
                ))
                .toList();

        return new MovieDetailResponse(
                resp.id(), resp.tmdbId(), resp.title(), resp.originalTitle(), resp.status(),
                resp.overview(), resp.posterUrl(), resp.releaseDate(), resp.runtime(),
                resp.genres(), resp.keywords(), resp.countryCode(), resp.countryName(),
                resp.certification(), resp.voteAverage(), resp.voteCount(), resp.popularity(),
                resp.budget(), resp.revenue(), resp.videos(), resp.posters(), resp.backdrops(),
                resp.castTop(), resp.crewCore(), resp.similar(), resp.recommendations(), storedMapped
        );
    }

    private String buildAvatarUrl(String root, String path) {
        if (path == null || path.isBlank()) return null;
        // TMDb는 '/xxx.jpg' 형태, Gravatar는 'https://...' 전체 URL일 수 있음
        return path.startsWith("/") ? (root + "/w185" + path) : path;
    }

    @Transactional(readOnly = true)
    public Page<MovieSummaryResponse> searchMovies(String q, boolean includeAdult, Pageable pageable) {
        String norm = (q == null) ? "" : q.trim();
        if (norm.isEmpty()) {
            Page<Movie> page = includeAdult
                    ? movieRepository.findAll(pageable)                 // 필요시 기본 정렬 포함
                    : movieRepository.findByAdultFalse(pageable);
            return page.map(this::toSummary);
        }
        return movieRepository.searchByText(norm, includeAdult, pageable)
                .map(this::toSummary);
    }

}
