package com.duck.moodflix.calendar.controller;

import com.duck.moodflix.calendar.dto.CalendarDtos;
import com.duck.moodflix.calendar.service.CalendarService;
import com.duck.moodflix.users.domain.entity.enums.UserStatus;
import com.duck.moodflix.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    /**
     * [수정] 캘린더 항목 공유 조회 (비인증 사용자 접근 가능하도록 SecurityConfig에서 열어줘야 함)
     * - @AuthenticationPrincipal 제거
     * - 에러 핸들링 로직 개선 (switchIfEmpty, onErrorResume)
     */
    @Operation(summary = "공유 UUID로 캘린더 항목 조회", description = "공유된 UUID를 기반으로 특정 캘린더 항목을 조회합니다. (인증 불필요)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "404", description = "항목을 찾을 수 없음"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    @GetMapping("/share/{uuid}")
    public Mono<ResponseEntity<CalendarDtos.EntryResponse>> getSharedCalendar(
            @Parameter(description = "공유용 UUID", required = true) @PathVariable String uuid) {

        return service.findByShareUuid(uuid)
                .map(ResponseEntity::ok) // 성공 시 200 OK와 entry 반환
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build())) // 비어있으면 404
                .onErrorResume(e -> {
                    // 에러 발생 시 로그를 남기고 500 반환
                    log.error("Error fetching shared calendar: uuid={}", uuid, e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
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
