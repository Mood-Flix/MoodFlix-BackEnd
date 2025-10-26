package com.duck.moodflix.calendar.service;

import com.duck.moodflix.calendar.domain.entity.CalendarEntry;
import com.duck.moodflix.calendar.dto.CalendarDtos;
import com.duck.moodflix.calendar.repository.CalendarEntryRepository;
import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.dto.response.MovieSummaryResponse;
import com.duck.moodflix.movie.repository.MovieRepository;
import com.duck.moodflix.recommend.domain.entity.Recommendation;
import com.duck.moodflix.recommend.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarService {

    private final CalendarEntryRepository repository;
    private final RecommendationRepository recommendationRepository;
    private final MovieRepository movieRepository;
    private final CalendarMapper calendarMapper;
    private final CalendarWriterService writerService;

    private static final int MAX_RECOMMENDATIONS = 5;

    public Mono<List<CalendarDtos.EntryResponse>> getEntriesByUserAndMonth(Long userId, int year, int month) {
        return Mono.fromCallable(() -> {
                    YearMonth yearMonth = YearMonth.of(year, month);
                    LocalDate startDate = yearMonth.atDay(1);
                    LocalDate endDate = yearMonth.atEndOfMonth();
                    return repository.findByUser_UserIdAndDateBetween(userId, startDate, endDate)
                            .stream()
                            .map(calendarMapper::toEntryResponse)
                            .collect(Collectors.toList());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<CalendarDtos.EntryResponse> getEntryByDate(Long userId, LocalDate date) {
        return Mono.fromCallable(() ->
                        repository.findByUser_UserIdAndDate(userId, date)
                                .map(calendarMapper::toEntryResponse)
                                .orElseGet(() -> calendarMapper.createEmptyEntryResponse(userId, date))
                )
                .subscribeOn(Schedulers.boundedElastic());
    }

    // [수정] shareUuid 기반 조회
    public Mono<CalendarDtos.EntryResponse> findByShareUuid(String shareUuid, Long userId) {
        return Mono.fromCallable(() -> {
                    Optional<CalendarEntry> entry = repository.findByShareUuid(shareUuid);
                    if (entry.isEmpty()) {
                        throw new RuntimeException("Calendar entry not found for shareUuid: " + shareUuid);
                    }
                    CalendarEntry calendarEntry = entry.get();
                    if (!calendarEntry.getUser().getUserId().equals(userId)) {
                        throw new SecurityException("Unauthorized access to calendar entry");
                    }
                    return calendarMapper.toEntryResponse(calendarEntry);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(e -> e);
    }

    public Mono<CalendarDtos.EntryResponse> saveOrUpdateEntry(Long userId, CalendarDtos.EntryRequest req) {
        log.info("CalendarService.saveOrUpdateEntry: userId={}, date={}, movieId={}",
                userId, req.date(), req.movieId());
        return Mono.fromCallable(() -> writerService.saveOrUpdateEntryBlocking(userId, req))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteEntryByDate(Long userId, LocalDate date) {
        return Mono.fromCallable(() -> {
                    writerService.deleteEntryByDateBlocking(userId, date);
                    return null;
                }).subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}