package com.duck.moodflix.calendar.service;

import com.duck.moodflix.calendar.dto.CalendarDtos;
import com.duck.moodflix.calendar.repository.CalendarEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarService {

    private final CalendarEntryRepository repository;
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

    // shareUuid 기반 조회
    public Mono<CalendarDtos.EntryResponse> findByShareUuid(String shareUuid) {
        return Mono.fromCallable(() -> {
                    if (shareUuid == null || shareUuid.trim().isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shareUuid");
                    }
                    // [추가] DB 조회 전 UUID 형식 검증
                    try {
                        UUID.fromString(shareUuid);
                    } catch (IllegalArgumentException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shareUuid format");
                    }
                    return repository.findByShareUuid(shareUuid)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Calendar entry not found for shareUuid: " + shareUuid));
                })
                // [수정] 람다 -> 메서드 참조로 변경
                .map(calendarMapper::toEntryResponse)
                .subscribeOn(Schedulers.boundedElastic());
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