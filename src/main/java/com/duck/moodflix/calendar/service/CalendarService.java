package com.duck.moodflix.calendar.service;

import com.duck.moodflix.calendar.domain.entity.CalendarEntry;
import com.duck.moodflix.calendar.dto.CalendarDtos;
import com.duck.moodflix.calendar.repository.CalendarEntryRepository;
import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.repository.MovieRepository;
import com.duck.moodflix.recommend.domain.entity.Recommendation;
import com.duck.moodflix.recommend.repository.RecommendationRepository;
import com.duck.moodflix.users.domain.entity.User;
import com.duck.moodflix.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers; // Schedulers 임포트

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarService {

    private final CalendarEntryRepository repository;
    private final UserRepository userRepository;
    private final RecommendationRepository recommendationRepository;
    private final MovieRepository movieRepository;
    private static final int MAX_RECOMMENDATIONS = 5;

    // [수정 1] 모든 DB 접근 코드를 fromCallable과 boundedElastic으로 감싸 논블로킹으로 처리
    public Mono<List<CalendarDtos.EntryResponse>> getEntriesByUserAndMonth(Long userId, int year, int month) {
        return Mono.fromCallable(() -> {
                    YearMonth yearMonth = YearMonth.of(year, month);
                    LocalDate startDate = yearMonth.atDay(1);
                    LocalDate endDate = yearMonth.atEndOfMonth();
                    return repository.findByUserUserIdAndDateBetween(userId, startDate, endDate)
                            .stream()
                            .map(this::mapToEntryResponse) // mapToEntryResponse도 블로킹이므로 이 안에서 실행
                            .collect(Collectors.toList());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<CalendarDtos.EntryResponse> getEntryByDate(Long userId, LocalDate date) {
        // [해결!] 블로킹 작업을 fromCallable로 감쌉니다.
        return Mono.fromCallable(() ->
                        repository.findFirstByUserUserIdAndDateOrderByCreatedAtDesc(userId, date)
                                .map(this::mapToEntryResponse)
                                // Optional이 비어있을 경우를 처리하는 로직은 여기에 포함합니다.
                                .orElseGet(() -> createEmptyEntryResponse(userId, date))
                )
                // 블로킹 작업 전용 스레드에서 실행하도록 지시합니다.
                .subscribeOn(Schedulers.boundedElastic());
    }

    // [수정 2] @Transactional이 붙은 블로킹 로직을 별도 private 메서드로 분리
    public Mono<CalendarDtos.EntryResponse> saveOrUpdateEntry(Long userId, CalendarDtos.EntryRequest req) {
        return Mono.fromCallable(() -> saveOrUpdateEntryBlocking(userId, req))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteEntryByDate(Long userId, LocalDate date) {
        return Mono.fromCallable(() -> {
                    deleteEntryByDateBlocking(userId, date);
                    return null; // Callable<Void>는 null을 리턴해야 함
                }).subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Transactional
    public void deleteEntryByDateBlocking(Long userId, LocalDate date) {
        CalendarEntry entry = repository.findFirstByUserUserIdAndDateOrderByCreatedAtDesc(userId, date)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found"));
        repository.delete(entry);
        log.info("Deleted CalendarEntry for userId={}, date={}", userId, date);
    }

    @Transactional
    public CalendarDtos.EntryResponse saveOrUpdateEntryBlocking(Long userId, CalendarDtos.EntryRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        CalendarEntry entry = repository.findByUser_UserIdAndDate(userId, req.date())
                .map(existingEntry -> {
                    existingEntry.updateNoteAndMood(req.note(), req.moodEmoji());
                    log.info("Updated CalendarEntry for userId={}, date={}", userId, req.date());
                    return existingEntry;
                })
                .orElseGet(() -> {
                    log.info("Created new CalendarEntry for userId={}, date={}", userId, req.date());
                    return CalendarEntry.builder()
                            .user(user)
                            .date(req.date())
                            .note(req.note())
                            .moodEmoji(req.moodEmoji())
                            .build();
                });

        try {
            // 2. 일단 저장을 시도함
            CalendarEntry savedEntry = repository.save(entry);
            return mapToEntryResponse(savedEntry);
        } catch (DataIntegrityViolationException e) {
            // 3. [핵심] 저장 실패 시 (충돌 발생 시), 다른 스레드가 방금 만든 데이터를 다시 조회
            log.warn("Race condition detected for userId={}, date={}. Retrying update.", userId, req.date());

            // 이제는 반드시 데이터가 있으므로 findBy...를 사용 (findFirst... 불필요)
            CalendarEntry existingEntryAfterConflict = repository.findByUser_UserIdAndDate(userId, req.date())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Entry not found after data integrity violation. Inconsistent state."));

            // 현재 요청의 데이터로 덮어쓴 후 다시 저장
            existingEntryAfterConflict.updateNoteAndMood(req.note(), req.moodEmoji());
            CalendarEntry updatedEntry = repository.save(existingEntryAfterConflict);
            return mapToEntryResponse(updatedEntry);
        }
    }

    // [수정 3] N+1 문제 해결
    private CalendarDtos.EntryResponse mapToEntryResponse(CalendarEntry entry) {
        List<CalendarDtos.RecommendationResponse> recommendationResponses = getRecommendationResponses(
                entry.getUser().getUserId(), entry.getDate());

        return new CalendarDtos.EntryResponse(
                entry.getId(),
                entry.getDate(),
                entry.getNote(),
                entry.getMoodEmoji(),
                recommendationResponses
        );
    }

    private CalendarDtos.EntryResponse createEmptyEntryResponse(Long userId, LocalDate date) {
        List<CalendarDtos.RecommendationResponse> recommendationResponses = getRecommendationResponses(userId, date);
        return new CalendarDtos.EntryResponse(null, date, null, null, recommendationResponses);
    }

    // N+1 해결을 위한 공통 로직 추출
    private List<CalendarDtos.RecommendationResponse> getRecommendationResponses(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59, 999999999);
        Pageable pageable = PageRequest.of(0, MAX_RECOMMENDATIONS);

        // 1. 추천 목록을 먼저 조회
        List<Recommendation> recommendations = recommendationRepository.findByUserUserIdAndCreatedAtBetween(
                userId, startOfDay, endOfDay, pageable);

        if (recommendations.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 추천 목록에서 영화 ID 리스트를 추출
        List<Long> movieIds = recommendations.stream()
                .map(Recommendation::getMovieId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 영화 ID 리스트를 사용해 영화 정보를 단 한 번의 쿼리로 조회
        Map<Long, Movie> movieMap = movieRepository.findAllById(movieIds).stream()
                .collect(Collectors.toMap(Movie::getId, Function.identity()));

        // 4. 조회된 영화 정보(Map)를 사용해 DTO 조립 (DB 추가 접근 없음)
        return recommendations.stream()
                .map(reco -> {
                    Movie movie = movieMap.get(reco.getMovieId());
                    return new CalendarDtos.RecommendationResponse(
                            reco.getId(),
                            reco.getMovieId(),
                            movie != null ? movie.getTitle() : "Unknown",
                            reco.getSimilarityScore(),
                            reco.getUserEmotionInput() != null ? reco.getUserEmotionInput().getInputText() : "Unknown"
                    );
                })
                .collect(Collectors.toList());
    }
}