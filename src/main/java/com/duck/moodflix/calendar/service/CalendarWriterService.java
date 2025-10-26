package com.duck.moodflix.calendar.service;

import com.duck.moodflix.calendar.domain.entity.CalendarEntry;
import com.duck.moodflix.calendar.dto.CalendarDtos;
import com.duck.moodflix.calendar.repository.CalendarEntryRepository;
import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.repository.MovieRepository;
import com.duck.moodflix.users.domain.entity.User;
import com.duck.moodflix.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarWriterService {

    private final CalendarEntryRepository repository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final CalendarMapper calendarMapper;

    @Transactional
    // 1. 메서드 시그니처를 CalendarService의 호출에 맞게 수정 (이름 + 파라미터)
    public CalendarDtos.EntryResponse saveOrUpdateEntryBlocking(Long userId, CalendarDtos.EntryRequest req) {

        // 2. req.id() 대신 (userId, req.date())로 기존 항목을 찾아야 합니다.
        Optional<CalendarEntry> existingEntry = repository.findByUser_UserIdAndDate(userId, req.date());
        CalendarEntry entryToSave;

        if (existingEntry.isPresent()) {
            // [업데이트]
            log.info("Updating existing entry: date={}, movieId={}", req.date(), req.movieId());
            entryToSave = existingEntry.get();
            entryToSave.setNote(req.note());
            entryToSave.setMoodEmoji(req.moodEmoji());
            updateMovieForEntry(entryToSave, req.movieId());

        } else {
            // [새로 생성]
            log.info("Creating new entry: date={}, movieId={}", req.date(), req.movieId());

            // 3. (필수) User 엔티티를 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));

            entryToSave = new CalendarEntry();

            // 4. (필수) User와 Date를 설정
            entryToSave.setUser(user);
            entryToSave.setDate(req.date());

            entryToSave.setNote(req.note());
            entryToSave.setMoodEmoji(req.moodEmoji());
            updateMovieForEntry(entryToSave, req.movieId());
            // PrePersist에서 shareUuid 자동 생성
        }

        entryToSave = repository.save(entryToSave);
        return calendarMapper.toEntryResponse(entryToSave);
    }

    @Transactional
    public void deleteEntryByDateBlocking(Long userId, LocalDate date) {
        CalendarEntry entry = repository.findByUser_UserIdAndDate(userId, date)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found"));
        repository.delete(entry);
        log.info("Deleted CalendarEntry: userId={}, date={}", userId, date);
    }

    private void updateMovieForEntry(CalendarEntry entry, Long movieId) {
        if (movieId != null) {
            Movie selectedMovie = movieRepository.findById(movieId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Movie not found: " + movieId));
            log.info("Setting movie: {} - {}", movieId, selectedMovie.getTitle());
            entry.updateMovie(selectedMovie);
        } else {
            log.info("Clearing movie selection");
            entry.updateMovie(null);
        }
    }
}
