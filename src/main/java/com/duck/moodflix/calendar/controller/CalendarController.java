package com.duck.moodflix.calendar.controller;

import com.duck.moodflix.calendar.dto.CalendarDtos;
import com.duck.moodflix.calendar.service.CalendarService;
import com.duck.moodflix.users.domain.entity.enums.UserStatus;
import com.duck.moodflix.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/calendar")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService service;
    private final UserRepository userRepository;

    // 월별 캘린더 데이터 조회
    @GetMapping
    public Mono<List<CalendarDtos.EntryResponse>> getEntries(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal User principal
    ) {
        validateYearMonth(year, month);
        return extractUserIdReactive(principal)
                .flatMap(userId -> service.getEntriesByUserAndMonth(userId, year, month));
    }

    @GetMapping("/share/{uuid}")
    public Mono<ResponseEntity<CalendarDtos.EntryResponse>> getSharedCalendar(@PathVariable String uuid) {
        return service.findByShareUuid(uuid)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(404).body(null))
                .onErrorResume(e -> {
                    if (e instanceof ResponseStatusException) {
                        ResponseStatusException ex = (ResponseStatusException) e;
                        return Mono.just(ResponseEntity.status(ex.getStatusCode()).body(null));
                    }
                    return Mono.just(ResponseEntity.status(500).body(null));
                });
    }

    // 특정 날짜의 캘린더 데이터 조회
    @GetMapping("/entry")
    public Mono<CalendarDtos.EntryResponse> getEntry(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal User principal
    ) {
        return extractUserIdReactive(principal)
                .flatMap(userId -> service.getEntryByDate(userId, date));
    }

    // 캘린더 데이터 저장/수정
    @PostMapping("/entry")
    public Mono<CalendarDtos.EntryResponse> saveOrUpdateEntry(
            @RequestBody CalendarDtos.EntryRequest req,
            @AuthenticationPrincipal User principal
    ) {
        return extractUserIdReactive(principal)
                .flatMap(userId -> service.saveOrUpdateEntry(userId, req));
    }

    // 캘린더 데이터 삭제
    @DeleteMapping("/entry")
    public Mono<Void> deleteEntry(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal User principal
    ) {
        return extractUserIdReactive(principal)
                .flatMap(userId -> service.deleteEntryByDate(userId, date));
    }

    /**
     * 동기 JPA(UserRepository) 호출을 boundedElastic로 오프로딩
     */
    private Mono<Long> extractUserIdReactive(User principal) {
        return Mono.fromCallable(() -> extractUserIdBlocking(principal))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * (블로킹) 사용자 ID 계산 로직
     */
    private Long extractUserIdBlocking(User principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }

        String username = principal.getUsername();
        if (isNumeric(username)) {
            Long userId = Long.parseLong(username);
            boolean active = userRepository.existsByUserIdAndStatus(userId, UserStatus.ACTIVE);
            if (!active) {
                log.warn("Inactive or non-existent user: userId={}", userId);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not active");
            }
            return userId;
        } else {
            return userRepository.findIdByEmailAndStatus(username, UserStatus.ACTIVE)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        }
    }

    private boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false;
        return true;
    }

    private void validateYearMonth(int year, int month) {
        if (year < 1970 || year > 2100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid year");
        }
        if (month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid month");
        }
    }
}
