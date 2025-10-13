package com.duck.moodflix.recommend.service;

import com.duck.moodflix.calendar.domain.entity.CalendarEntry;
import com.duck.moodflix.calendar.repository.CalendarEntryRepository;
import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.dto.response.MovieSummaryResponse;
import com.duck.moodflix.movie.repository.MovieRepository;
import com.duck.moodflix.recommend.client.ModelServerClient;
import com.duck.moodflix.recommend.domain.entity.Recommendation;
import com.duck.moodflix.recommend.domain.entity.UserEmotionInput;
import com.duck.moodflix.recommend.dto.RecommendDtos;
import com.duck.moodflix.recommend.repository.RecommendationRepository;
import com.duck.moodflix.recommend.repository.UserEmotionInputRepository;
import com.duck.moodflix.users.domain.entity.User;
import com.duck.moodflix.users.domain.entity.enums.Role;
import com.duck.moodflix.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendService {

    private static final Logger log = LoggerFactory.getLogger(RecommendService.class);
    private static final int MAX_DAILY_RECOMMENDATIONS = 5;

    private final ModelServerClient modelClient;
    private final UserEmotionInputRepository inputRepo;
    private final RecommendationRepository recRepo;
    private final UserRepository userRepo;
    private final MovieRepository movieRepo;
    private final CalendarEntryRepository calendarEntryRepository;

    public Mono<RecommendDtos.Response> byText(Long userId, RecommendDtos.Request req) {
        if (userId == null) {
            return Mono.error(new IllegalArgumentException("userId is required"));
        }

        // [수정 1] subscribeOn 체이닝 오류 수정 및 관리자 제한 해제 로직 추가
        return Mono.fromCallable(() -> {
                    // 사용자 조회 및 역할 확인 (블로킹)
                    User user = userRepo.findById(userId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                    // 관리자일 경우, 횟수 제한 로직을 건너뜀
                    if (user.getRole() == Role.USER.ADMIN) {
                        log.info("Admin user [{}] - bypassing recommendation limit.", userId);
                        return -1L; // 관리자는 제한 없음을 의미하는 값
                    }

                    // 일반 사용자일 경우, 당일 추천 횟수 확인 (블로킹)
                    LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
                    LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59, 999999999);
                    return recRepo.countByUserUserIdAndCreatedAtBetween(userId, startOfDay, endOfDay);
                })
                .subscribeOn(Schedulers.boundedElastic()) // 올바른 체이닝
                .flatMap(count -> {
                    // 관리자가 아닐 경우에만 횟수 체크
                    if (count != -1L && count >= MAX_DAILY_RECOMMENDATIONS) {
                        return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                                "Daily recommendation limit (" + MAX_DAILY_RECOMMENDATIONS + ") reached"));
                    }

                    int topN;
                    if (count == -1L) { // 관리자
                        topN = Optional.ofNullable(req.topN()).orElse(20);
                    } else { // 일반 사용자
                        topN = Math.min(Optional.ofNullable(req.topN()).orElse(20), MAX_DAILY_RECOMMENDATIONS - count.intValue());
                    }
                    String text = Optional.ofNullable(req.text()).orElse("");

                    // 모델 서버 호출 (논블로킹) -> 결과 저장 (블로킹)
                    return modelClient.recommendByText(text, topN)
                            .flatMap(res -> saveAllReactive(userId, text, res));
                });
    }

    private Mono<RecommendDtos.Response> saveAllReactive(Long userId, String text, ModelServerClient.ModelRecommendResponse res) {
        return Mono.fromCallable(() -> saveAllBlocking(userId, text, res))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(error -> log.error("Error during saveAll operation: {}", error.getMessage(), error))
                .onErrorMap(e -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save recommendation data.", e));
    }

    @Transactional
    public RecommendDtos.Response saveAllBlocking(Long userId, String text, ModelServerClient.ModelRecommendResponse res) {
        log.debug("Saving recommendation for userId={}, text={}", userId, text);
        User userRef = userRepo.getReferenceById(userId);

        UserEmotionInput input = inputRepo.save(UserEmotionInput.builder()
                .user(userRef)
                .inputText(text)
                .build());

        List<ModelServerClient.ModelRecommendItem> topItems = res.items().stream()
                .sorted((r1, r2) -> Double.compare(r2.similarity(), r1.similarity()))
                .limit(MAX_DAILY_RECOMMENDATIONS)
                .collect(Collectors.toList());

        List<Recommendation> recommendations = topItems.stream()
                .map(it -> Recommendation.builder()
                        .user(userRef)
                        .userEmotionInput(input)
                        .movieId(it.movie_id())
                        .similarityScore(it.similarity())
                        .build())
                .collect(Collectors.toList());
        recRepo.saveAll(recommendations);

        saveOrUpdateCalendarEntry(userId, text, userRef);

        List<Long> movieIds = topItems.stream().map(ModelServerClient.ModelRecommendItem::movie_id).toList();
        Map<Long, Movie> movieMap = movieRepo.findByIdIn(movieIds).stream()
                .collect(Collectors.toMap(Movie::getId, Function.identity()));

        List<RecommendDtos.RecommendItemResponse> items = topItems.stream()
                .map(it -> {
                    Movie movie = movieMap.get(it.movie_id());
                    MovieSummaryResponse movieSummary;
                    if (movie != null) {
                        movieSummary = new MovieSummaryResponse(
                                movie.getId(), movie.getTmdbId(), movie.getTitle(), movie.getPosterUrl(),
                                movie.getGenre(), movie.getReleaseDate(), movie.getVoteAverage()
                        );
                    } else {
                        log.warn("Movie not found in DB for movieId={}, using fallback data", it.movie_id());
                        movieSummary = new MovieSummaryResponse(
                                it.movie_id(), null, it.title(), null,
                                it.genres().stream().findFirst().orElse(null), null, null
                        );
                    }
                    return new RecommendDtos.RecommendItemResponse(movieSummary, it.similarity());
                })
                .toList();

        return new RecommendDtos.Response(res.version(), items, input.getId());
    }

    private void saveOrUpdateCalendarEntry(Long userId, String text, User userRef) {
        LocalDate today = LocalDate.now();
        CalendarEntry entry = calendarEntryRepository.findFirstByUserUserIdAndDateOrderByCreatedAtDesc(userId, today)
                .orElseGet(() -> {
                    log.info("Creating new CalendarEntry for userId={}, date={}", userId, today);
                    return CalendarEntry.builder()
                            .user(userRef)
                            .date(today)
                            .userInputText(text) // 생성 시에만 텍스트 설정
                            .build();
                });

        calendarEntryRepository.save(entry);
    }
}