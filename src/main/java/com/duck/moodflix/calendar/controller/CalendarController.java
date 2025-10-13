package com.duck.moodflix.calendar.controller;

import com.duck.moodflix.calendar.domain.entity.CalendarEntry;
import com.duck.moodflix.calendar.dto.CalendarDtos;
import com.duck.moodflix.calendar.service.CalendarService;
import com.duck.moodflix.users.domain.entity.enums.UserStatus;
import com.duck.moodflix.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

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
            @AuthenticationPrincipal User principal) {
        Long userId = extractUserId(principal);
        return service.getEntriesByUserAndMonth(userId, year, month);
    }

    // 특정 날짜의 캘린더 데이터 조회
    @GetMapping("/entry")
    public Mono<CalendarDtos.EntryResponse> getEntry(
            @RequestParam String date,
            @AuthenticationPrincipal User principal) {
        Long userId = extractUserId(principal);
        return service.getEntryByDate(userId, LocalDate.parse(date));
    }

    // 캘린더 데이터 저장/수정
    @PostMapping("/entry")
    public Mono<CalendarDtos.EntryResponse> saveOrUpdateEntry(
            @RequestBody CalendarDtos.EntryRequest req,
            @AuthenticationPrincipal User principal) {
        Long userId = extractUserId(principal);
        return service.saveOrUpdateEntry(userId, req);
    }

    // 캘린더 데이터 삭제
    @DeleteMapping("/entry")
    public Mono<Void> deleteEntry(
            @RequestParam String date,
            @AuthenticationPrincipal User principal) {
        Long userId = extractUserId(principal);
        return service.deleteEntryByDate(userId, LocalDate.parse(date));
    }

    private Long extractUserId(User principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }

        String username = principal.getUsername();
        Long userId;

        if (isNumeric(username)) {
            userId = Long.parseLong(username);
            if (!userRepository.existsByUserIdAndStatus(userId, UserStatus.ACTIVE)) {
                log.warn("Inactive or non-existent user: userId={}", userId);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not active");
            }
        } else {
            userId = userRepository.findIdByEmailAndStatus(username, UserStatus.ACTIVE)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        }

        return userId;
    }

    private boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false;
        return true;
    }
}