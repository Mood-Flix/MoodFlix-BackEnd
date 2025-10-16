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

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarWriterService {

    private final CalendarEntryRepository repository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final CalendarMapper calendarMapper;

    @Transactional
    public CalendarDtos.EntryResponse saveOrUpdateEntryBlocking(Long userId, CalendarDtos.EntryRequest req) {
        log.info("=== CalendarWriterService.saveOrUpdateEntryBlocking START ===");
        // [수정] note와 moodEmoji 로그 제거
        log.info("Request: userId={}, date={}, movieId={} (note/mood redacted)",
                userId, req.date(), req.movieId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Optional<CalendarEntry> existingEntryOpt = repository.findByUser_UserIdAndDate(userId, req.date());
        log.info("Existing entry found: {}", existingEntryOpt.isPresent());

        CalendarEntry entryToSave;

        if (existingEntryOpt.isPresent()) {
            CalendarEntry existingEntry = existingEntryOpt.get();
            boolean isNoteSame = Objects.equals(existingEntry.getNote(), req.note());
            boolean isMoodSame = Objects.equals(existingEntry.getMoodEmoji(), req.moodEmoji());
            boolean isMovieSame = Objects.equals(
                    existingEntry.getMovie() != null ? existingEntry.getMovie().getId() : null,
                    req.movieId()
            );

            if (isNoteSame && isMoodSame && isMovieSame) {
                log.info("No changes detected, returning existing entry");
                return calendarMapper.toEntryResponse(existingEntry);
            } else {
                // [수정] note와 moodEmoji 로그 제거
                log.info("Updating existing entry: movieId={} (note/mood redacted)",
                        req.movieId());
                existingEntry.updateNoteAndMood(req.note(), req.moodEmoji());
                updateMovieForEntry(existingEntry, req.movieId());
                entryToSave = existingEntry;
            }
        } else {
            // [수정] note와 moodEmoji 로그 제거
            log.info("Creating NEW entry: movieId={} (note/mood redacted)",
                    req.movieId());
            CalendarEntry newEntry = CalendarEntry.builder()
                    .user(user)
                    .date(req.date())
                    .note(req.note())
                    .moodEmoji(req.moodEmoji())
                    .build();
            updateMovieForEntry(newEntry, req.movieId());
            entryToSave = newEntry;
        }

        try {
            CalendarEntry savedEntry = repository.save(entryToSave);
            log.info(" SAVED Successfully! Entry ID: {}", savedEntry.getId());
            return calendarMapper.toEntryResponse(savedEntry);
        } catch (DataIntegrityViolationException e) {
            log.error("DB Constraint Violation: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate calendar entry", e);
        } catch (Exception e) {
            log.error("Save Failed: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save entry", e);
        }
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
