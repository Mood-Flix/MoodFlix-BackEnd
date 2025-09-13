package com.duck.moodflix.movie.client;

import com.duck.moodflix.auth.config.TMDbProperties;
import com.duck.moodflix.movie.dto.tmdb.TMDbMovieDetailDto;
import com.duck.moodflix.movie.dto.tmdb.TMDbMovieListResponse;
import com.duck.moodflix.movie.dto.tmdb.reviews.ReviewsPageDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;

@Component
@Slf4j
public class TMDbClient {

    private final WebClient webClient;
    private final TMDbProperties tmdbProperties;

    public TMDbClient(@Qualifier("tmdbWebClient") WebClient webClient,
                      TMDbProperties tmdbProperties) {
        this.webClient = webClient;
        this.tmdbProperties = tmdbProperties;
    }

    /** 인기영화 페이지 조회 (언어: ko-KR) */
    public TMDbMovieListResponse getPopular(int page) {
        return webClient.get()
                .uri(b -> withApiKey(
                        b.path("/movie/popular")
                                .queryParam("language", "ko-KR")
                                .queryParam("page", page)
                ))
                .retrieve()
                .onStatus(s -> s.value() == 429, rsp -> {
                    log.warn("TMDb rate limited: GET /movie/popular page={}", page);
                    return rsp.createException();
                })
                .onStatus(s -> s.is5xxServerError(), rsp -> rsp.createException())
                .bodyToMono(TMDbMovieListResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                        .filter(ex -> ex instanceof WebClientResponseException.TooManyRequests))
                .timeout(Duration.ofSeconds(6))
                .block();
    }
    public boolean hasKoreanTranslation(long tmdbId) {
        JsonNode root = webClient.get()
                .uri(b -> withApiKey(b.path("/movie/{id}/translations"), tmdbId))
                .retrieve()
                .onStatus(s -> s.value() == 429, rsp -> rsp.createException())
                .onStatus(s -> s.is5xxServerError(), rsp -> rsp.createException())
                .bodyToMono(JsonNode.class)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                        .filter(ex -> ex instanceof WebClientResponseException.TooManyRequests))
                .timeout(Duration.ofSeconds(6))
                .block();

        if (root == null || !root.has("translations")) return false;
        for (JsonNode t : root.get("translations")) {
            String c = t.path("iso_3166_1").asText("");
            String l = t.path("iso_639_1").asText("");
            if ("KR".equalsIgnoreCase(c) && "ko".equalsIgnoreCase(l)) {
                JsonNode data = t.path("data");
                String title = data.path("title").asText("");
                String overview = data.path("overview").asText("");
                return !title.isBlank() || !overview.isBlank();
            }
        }
        return false;
    }

    /** 상세(ko-KR 우선) */
    public TMDbMovieDetailDto getMovieDetail(Long tmdbId) {
        return getMovieDetail(tmdbId, "ko-KR");
    }

    /** 상세(언어 지정) */
    public TMDbMovieDetailDto getMovieDetail(Long tmdbId, String language) {
        return webClient.get()
                .uri(b -> withApiKey(
                        b.path("/movie/{id}")
                                .queryParam("language", language)
                                .queryParam("append_to_response",
                                        "keywords,credits,reviews,release_dates,images,videos,similar,recommendations")
                                .queryParam("reviews.page", 1)
                                .queryParam("include_image_language", "ko,null,en")
                                .queryParam("include_video_language", "ko,en,null"),
                                tmdbId
                ))
                .retrieve()
                .onStatus(s -> s.value() == 429, rsp -> {
                    log.warn("TMDb rate limited: GET /movie/{} detail", tmdbId);
                    return rsp.createException();
                })
                .onStatus(s -> s.is5xxServerError(), rsp -> rsp.createException())
                .bodyToMono(TMDbMovieDetailDto.class)
                .timeout(Duration.ofSeconds(6)) // 먼저 타임아웃
                .retryWhen(
                        Retry.backoff(3, Duration.ofMillis(500))
                                .filter(ex -> ex instanceof WebClientResponseException.TooManyRequests
                                        || ex instanceof java.util.concurrent.TimeoutException)
                )
                .block(); // 명시적 블록 타임아웃
    }

    // TMDbClient.java
    public TMDbMovieListResponse discoverByYear(int year, int page) {
        return webClient.get()
                .uri(b -> withApiKey(
                        b.path("/discover/movie")
                                .queryParam("sort_by", "popularity.desc")
                                .queryParam("include_adult", false)
                                .queryParam("include_video", false)
                                .queryParam("language", "ko-KR")
                                .queryParam("primary_release_year", year)
                                .queryParam("page", page)
                ))
                 .retrieve()
                 .onStatus(s -> s.value() == 429, rsp -> {
                  log.warn("TMDb rate limited: GET /discover/movie year={} page={}", year, page);
                   return rsp.createException();
                })
                .onStatus(s -> s.is5xxServerError(), rsp -> rsp.createException())
                .bodyToMono(TMDbMovieListResponse.class)
                .timeout(Duration.ofSeconds(6))
                .retryWhen(
                        Retry.backoff(3, Duration.ofMillis(500))
                            .filter(ex -> ex instanceof WebClientResponseException.TooManyRequests
                                    || ex instanceof java.util.concurrent.TimeoutException)
                )
                .block();
    }


    /** 리뷰 페이지 조회 */
    public ReviewsPageDto getReviews(Long tmdbId, String lang, int page) {
        return webClient.get()
                .uri(b -> withApiKey(
                        b.path("/movie/{movieId}/reviews")
                                .queryParam("language", lang)
                                .queryParam("page", page),
                        tmdbId
                ))
                .retrieve()
                .onStatus(s -> s.value() == 429, rsp -> rsp.createException())
                .onStatus(s -> s.is5xxServerError(), rsp -> rsp.createException())
                .bodyToMono(ReviewsPageDto.class)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                        .filter(ex -> ex instanceof WebClientResponseException.TooManyRequests))
                .timeout(Duration.ofSeconds(6))
                .block();
    }

    /**
     * v4 Bearer 토큰이 없을 때만 v3 api_key를 쿼리스트링에 추가.
     * path 변수는 여기서 build 처리.
     */
    private URI withApiKey(UriBuilder ub, Object... uriVars) {
        boolean useQueryApiKey = tmdbProperties.getBearerToken() == null
                || tmdbProperties.getBearerToken().isBlank();

        if (useQueryApiKey && tmdbProperties.getApiKey() != null
                && !tmdbProperties.getApiKey().isBlank()) {
            ub.queryParam("api_key", tmdbProperties.getApiKey());
        }
        return (uriVars == null || uriVars.length == 0) ? ub.build() : ub.build(uriVars);
    }
}
