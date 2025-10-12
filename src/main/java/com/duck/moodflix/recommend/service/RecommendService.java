package com.duck.moodflix.recommend.service;

import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.dto.response.MovieSummaryResponse;
import com.duck.moodflix.recommend.client.ModelServerClient;
import com.duck.moodflix.recommend.domain.entity.Recommendation;
import com.duck.moodflix.recommend.domain.entity.UserEmotionInput;
import com.duck.moodflix.recommend.dto.RecommendDtos;
import com.duck.moodflix.recommend.repository.RecommendationRepository;
import com.duck.moodflix.recommend.repository.UserEmotionInputRepository;
import com.duck.moodflix.users.repository.UserRepository;
import com.duck.moodflix.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendService {

    private static final Logger log = LoggerFactory.getLogger(RecommendService.class);

    private final ModelServerClient modelClient;
    private final UserEmotionInputRepository inputRepo;
    private final RecommendationRepository recRepo;
    private final UserRepository userRepo;
    private final MovieRepository movieRepo;

    public record Request(String text, Integer topN) {}
    public record Item(Long movieId, String title, List<String> genres, double similarity, String posterUrl) {}
    public record Response(String version, List<Item> items, Long logId) {}

    public Mono<RecommendDtos.Response> byText(Long userId, RecommendDtos.Request req) {
        if (userId == null) throw new IllegalArgumentException("userId is required");

        String text = Optional.ofNullable(req.text()).orElse("");
        int topN = Optional.ofNullable(req.topN()).orElse(20);

        return modelClient.recommendByText(text, topN)
                .publishOn(Schedulers.boundedElastic()) // 리액티브 + JPA 안전 구역
                .map(res -> saveAll(userId, text, res))
                .doOnError(error -> log.error("Error in byText: {}", error.getMessage(), error));
    }

    @Transactional
    protected RecommendDtos.Response saveAll(Long userId, String text, ModelServerClient.ModelRecommendResponse res) {
        log.debug("Saving recommendation for userId={}, text={}", userId, text);
        var userRef = userRepo.getReferenceById(userId); // 존재 보장 가정
        var input = inputRepo.save(UserEmotionInput.builder()
                .user(userRef) // 연관 주입 (FK not null)
                .inputText(text)
                .build());

        var rows = new ArrayList<Recommendation>();
        res.items().forEach(it -> rows.add(
                Recommendation.builder()
                        .user(userRef) // 연관 주입
                        .userEmotionInput(input) // 연관 주입
                        .movieId(it.movie_id())
                        .similarityScore(it.similarity())
                        .build()
        ));
        recRepo.saveAll(rows);

        // 영화 정보 조회
        List<Long> movieIds = res.items().stream()
                .map(ModelServerClient.ModelRecommendItem::movie_id)
                .collect(Collectors.toList());
        List<Movie> movies = movieRepo.findByIdIn(movieIds);
        log.debug("Found {} movies for IDs: {}", movies.size(), movieIds);

        var items = res.items().stream()
                .map(it -> {
                    Optional<Movie> movie = movies.stream()
                            .filter(m -> m.getId().equals(it.movie_id()))
                            .findFirst();
                    MovieSummaryResponse movieSummary = movie
                            .map(m -> new MovieSummaryResponse(
                                    m.getId(),
                                    m.getTmdbId(),
                                    m.getTitle(),
                                    m.getPosterUrl(),
                                    m.getGenre(),
                                    m.getReleaseDate(),
                                    m.getVoteAverage()
                            ))
                            .orElseGet(() -> {
                                log.warn("Movie not found in DB for movieId={}, using fallback data", it.movie_id());
                                return new MovieSummaryResponse(
                                        Long.valueOf(it.movie_id()), // long -> Long
                                        null, // tmdbId: DB 없음
                                        it.title(),
                                        null, // posterUrl
                                        it.genres().stream().findFirst().orElse(null),
                                        null, // releaseDate
                                        null // voteAverage
                                );
                            });
                    return new RecommendDtos.RecommendItemResponse(movieSummary, it.similarity());
                })
                .toList();

        return new RecommendDtos.Response(res.version(), items, input.getId());
    }
}