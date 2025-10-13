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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
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

    // 월별 엔트리 조회
    public Mono<List<CalendarDtos.EntryResponse>> getEntriesByUserAndMonth(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        return Mono.just(repository.findByUserUserIdAndDateBetween(userId, startDate, endDate).stream()
                .map(this::mapToEntryResponse)
                .collect(Collectors.toList()));
    }

    // 특정 날짜의 엔트리 조회
    public Mono<CalendarDtos.EntryResponse> getEntryByDate(Long userId, LocalDate date) {
        return Mono.justOrEmpty(repository.findFirstByUserUserIdAndDateOrderByCreatedAtDesc(userId, date))
                .map(this::mapToEntryResponse)
                .switchIfEmpty(Mono.just(createEmptyEntryResponse(userId, date)));
    }

    // 엔트리 저장/수정
    @Transactional
    public Mono<CalendarDtos.EntryResponse> saveOrUpdateEntry(Long userId, CalendarDtos.EntryRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        try {
            return Mono.justOrEmpty(repository.findFirstByUserUserIdAndDateOrderByCreatedAtDesc(userId, req.date()))
                    .map(entry -> {
                        entry.updateNoteAndMood(req.note(), req.moodEmoji());
                        repository.save(entry);
                        log.info("Updated CalendarEntry for userId={}, date={}", userId, req.date());
                        return entry;
                    })
                    .switchIfEmpty(Mono.just(repository.save(CalendarEntry.builder()
                            .user(user)
                            .date(req.date())
                            .note(req.note())
                            .moodEmoji(req.moodEmoji())
                            .movie(null)
                            .recommendation(null)
                            .userInputText(null)
                            .build())))
                    .doOnSuccess(entry -> log.info("Created new CalendarEntry for userId={}, date={}", userId, req.date()))
                    .map(this::mapToEntryResponse);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate CalendarEntry attempted for userId={}, date={}. Updating existing.", userId, req.date());
            return Mono.justOrEmpty(repository.findFirstByUserUserIdAndDateOrderByCreatedAtDesc(userId, req.date()))
                    .map(entry -> {
                        entry.updateNoteAndMood(req.note(), req.moodEmoji());
                        repository.save(entry);
                        log.info("Updated existing CalendarEntry for userId={}, date={}", userId, req.date());
                        return entry;
                    })
                    .map(this::mapToEntryResponse)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "CalendarEntry not found after duplicate attempt")));
        }
    }

    // 엔트리 삭제
    @Transactional
    public Mono<Void> deleteEntryByDate(Long userId, LocalDate date) {
        return Mono.justOrEmpty(repository.findFirstByUserUserIdAndDateOrderByCreatedAtDesc(userId, date))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found")))
                .doOnNext(repository::delete)
                .doOnSuccess(v -> log.info("Deleted CalendarEntry for userId={}, date={}", userId, date))
                .then();
    }

    // CalendarEntry를 EntryResponse로 변환
    private CalendarDtos.EntryResponse mapToEntryResponse(CalendarEntry entry) {
        LocalDateTime startOfDay = entry.getDate().atStartOfDay();
        LocalDateTime endOfDay = entry.getDate().atTime(23, 59, 59, 999999999);
        Pageable pageable = PageRequest.of(0, MAX_RECOMMENDATIONS);

        List<Recommendation> recommendations = recommendationRepository.findByUserUserIdAndCreatedAtBetween(
                entry.getUser().getUserId(), startOfDay, endOfDay, pageable);

        log.info("Recommendations for userId={}, date={}: total={}",
                entry.getUser().getUserId(), entry.getDate(), recommendations.size());

        List<CalendarDtos.RecommendationResponse> recommendationResponses = recommendations.stream()
                .map(reco -> {
                    Movie movie = movieRepository.findById(reco.getMovieId()).orElse(null);
                    return new CalendarDtos.RecommendationResponse(
                            reco.getId(),
                            reco.getMovieId(),
                            movie != null ? movie.getTitle() : "Unknown",
                            reco.getSimilarityScore(),
                            reco.getUserEmotionInput() != null ? reco.getUserEmotionInput().getInputText() : "Unknown"
                    );
                })
                .collect(Collectors.toList());

        return new CalendarDtos.EntryResponse(
                entry.getId(),
                entry.getDate(),
                entry.getNote(),
                entry.getMoodEmoji(),
                recommendationResponses
        );
    }

    // CalendarEntry가 없을 경우 빈 응답 생성
    private CalendarDtos.EntryResponse createEmptyEntryResponse(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59, 999999999);
        Pageable pageable = PageRequest.of(0, MAX_RECOMMENDATIONS);

        List<Recommendation> recommendations = recommendationRepository.findByUserUserIdAndCreatedAtBetween(
                userId, startOfDay, endOfDay, pageable);

        log.info("Empty entry recommendations for userId={}, date={}: total={}",
                userId, date, recommendations.size());

        List<CalendarDtos.RecommendationResponse> recommendationResponses = recommendations.stream()
                .map(reco -> {
                    Movie movie = movieRepository.findById(reco.getMovieId()).orElse(null);
                    return new CalendarDtos.RecommendationResponse(
                            reco.getId(),
                            reco.getMovieId(),
                            movie != null ? movie.getTitle() : "Unknown",
                            reco.getSimilarityScore(),
                            reco.getUserEmotionInput() != null ? reco.getUserEmotionInput().getInputText() : "Unknown"
                    );
                })
                .collect(Collectors.toList());

        return new CalendarDtos.EntryResponse(
                null,
                date,
                null,
                null,
                recommendationResponses
        );
    }
}