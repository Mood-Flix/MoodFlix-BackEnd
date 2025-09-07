package com.duck.moodflix.movie.client;

import com.duck.moodflix.auth.config.TMDbProperties;
import com.duck.moodflix.movie.dto.tmdb.TMDbMovieDetailDto;
import com.duck.moodflix.movie.dto.tmdb.TMDbMovieListResponse;
import com.duck.moodflix.movie.dto.tmdb.reviews.ReviewsPageDto;
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
                .retryWhen(Retry.backoff(3, Duration.ofMillis(300))
                        .filter(ex -> ex instanceof WebClientResponseException.TooManyRequests))
                .timeout(Duration.ofSeconds(5))
                .block();
    }

    public TMDbMovieDetailDto getMovieDetail(Long tmdbId) {
        return webClient.get()
                .uri(b -> withApiKey(
                        b.path("/movie/{id}")
                                .queryParam("language", "ko-KR")
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
                .retryWhen(Retry.backoff(3, Duration.ofMillis(300))
                        .filter(ex -> ex instanceof WebClientResponseException.TooManyRequests))
                .timeout(Duration.ofSeconds(8))
                .block();
    }

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
                .retryWhen(Retry.backoff(3, Duration.ofMillis(300))
                        .filter(ex -> ex instanceof WebClientResponseException.TooManyRequests))
                .timeout(Duration.ofSeconds(5))
                .block();
    }

    /** v4 Bearer가 없을 때만 v3 api_key를 쿼리파라미터로 추가. path 변수는 여기서 build 처리 */
    private URI withApiKey(UriBuilder ub, Object... uriVars) {
        boolean useQueryApiKey = tmdbProperties.getBearerToken() == null
                || tmdbProperties.getBearerToken().isBlank();

        if (useQueryApiKey && tmdbProperties.getApiKey() != null
                && !tmdbProperties.getApiKey().isBlank()) {
            ub.queryParam("api_key", tmdbProperties.getApiKey());
        }

        return (uriVars == null || uriVars.length == 0)
                ? ub.build()
                : ub.build(uriVars);
    }
}