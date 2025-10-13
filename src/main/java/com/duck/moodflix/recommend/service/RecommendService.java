package com.duck.moodflix.recommend.service;

import com.duck.moodflix.calendar.domain.entity.CalendarEntry;
import com.duck.moodflix.calendar.repository.CalendarEntryRepository;
import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.dto.response.MovieSummaryResponse;
import com.duck.moodflix.recommend.client.ModelServerClient;
import com.duck.moodflix.recommend.domain.entity.Recommendation;
import com.duck.moodflix.recommend.domain.entity.UserEmotionInput;
import com.duck.moodflix.recommend.dto.RecommendDtos;
import com.duck.moodflix.recommend.repository.RecommendationRepository;
import com.duck.moodflix.recommend.repository.UserEmotionInputRepository;
import com.duck.moodflix.users.domain.entity.User;
import com.duck.moodflix.users.repository.UserRepository;
import com.duck.moodflix.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    public record Request(String text, Integer topN) {}
    public record Item(Long movieId, String title, List<String> genres, double similarity, String posterUrl) {}
    public record Response(String version, List<Item> items, Long logId) {}

    public Mono<RecommendDtos.Response> byText(Long userId, RecommendDtos.Request req) {
        if (userId == null) throw new IllegalArgumentException("userId is required");

        // 당일 추천 횟수 확인
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59, 999999999);
        long count = recRepo.countByUserUserIdAndCreatedAtBetween(userId, startOfDay, endOfDay);
        if (count >= MAX_DAILY_RECOMMENDATIONS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Daily recommendation limit (" + MAX_DAILY_RECOMMENDATIONS + ") reached");
        }

        String text = Optional.ofNullable(req.text()).orElse("");
        int topN = Math.min(Optional.ofNullable(req.topN()).orElse(20), MAX_DAILY_RECOMMENDATIONS - (int) count);

        return modelClient.recommendByText(text, topN)
                .publishOn(Schedulers.boundedElastic())
                .map(res -> saveAll(userId, text, res))
                .doOnError(error -> log.error("Error in byText: {}", error.getMessage(), error))
                .onErrorMap(DataIntegrityViolationException.class, e ->
                        new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate calendar entry detected", e));
    }

    @Transactional
    protected RecommendDtos.Response saveAll(Long userId, String text, ModelServerClient.ModelRecommendResponse res) {
        log.debug("Saving recommendation for userId={}, text={}", userId, text);
        var userRef = userRepo.getReferenceById(userId);

        var input = inputRepo.save(UserEmotionInput.builder()
                .user(userRef)
                .inputText(text)
                .build());

        List<ModelServerClient.ModelRecommendItem> topItems = res.items().stream()
                .sorted((r1, r2) -> Double.compare(r2.similarity(), r1.similarity()))
                .limit(MAX_DAILY_RECOMMENDATIONS)
                .collect(Collectors.toList());

        var rows = new ArrayList<Recommendation>();
        topItems.forEach(it -> rows.add(
                Recommendation.builder()
                        .user(userRef)
                        .userEmotionInput(input)
                        .movieId(it.movie_id())
                        .similarityScore(it.similarity())
                        .build()
        ));
        recRepo.saveAll(rows);

        // CalendarEntry 생성/업데이트
        LocalDate today = LocalDate.now();
        Optional<CalendarEntry> existingEntry = calendarEntryRepository
                .findFirstByUserUserIdAndDateOrderByCreatedAtDesc(userId, today);

        try {
            if (existingEntry.isPresent()) {
                CalendarEntry entry = existingEntry.get();
                entry.updateNoteAndMood(null, null);
                calendarEntryRepository.save(entry);
                log.info("Updated CalendarEntry for userId={}, date={}", userId, today);
            } else {
                calendarEntryRepository.save(
                        CalendarEntry.builder()
                                .user(userRef)
                                .date(today)
                                .userInputText(text)
                                .movie(null)
                                .recommendation(null)
                                .note(null)
                                .moodEmoji(null)
                                .build()
                );
                log.info("Created new CalendarEntry for userId={}, date={}", userId, today);
            }
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate CalendarEntry attempted for userId={}, date={}. Attempting to update existing.", userId, today);
            CalendarEntry existing = calendarEntryRepository
                    .findFirstByUserUserIdAndDateOrderByCreatedAtDesc(userId, today)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "CalendarEntry not found after duplicate attempt"));
            existing.updateNoteAndMood(null, null);
            calendarEntryRepository.save(existing);
            log.info("Updated existing CalendarEntry after duplicate attempt for userId={}, date={}", userId, today);
        }

        List<Long> movieIds = topItems.stream()
                .map(ModelServerClient.ModelRecommendItem::movie_id)
                .collect(Collectors.toList());
        List<Movie> movies = movieRepo.findByIdIn(movieIds);
        log.debug("Found {} movies for IDs: {}", movies.size(), movieIds);

        var items = topItems.stream()
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
                                        Long.valueOf(it.movie_id()),
                                        null,
                                        it.title(),
                                        null,
                                        it.genres().stream().findFirst().orElse(null),
                                        null,
                                        null
                                );
                            });
                    return new RecommendDtos.RecommendItemResponse(movieSummary, it.similarity());
                })
                .toList();

        return new RecommendDtos.Response(res.version(), items, input.getId());
    }
}