package com.duck.moodflix.calendar.repository;

import com.duck.moodflix.calendar.domain.entity.CalendarEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarEntryRepository extends JpaRepository<CalendarEntry, Long> {
    List<CalendarEntry> findByUserUserId(Long userId);
    Optional<CalendarEntry> findByIdAndUserUserId(Long id, Long userId);
    List<CalendarEntry> findByUserUserIdAndDate(Long userId, LocalDate date);  // ✅ List로 변경 (중복 허용)
    List<CalendarEntry> findByUserUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    Optional<CalendarEntry> findFirstByUserUserIdAndDateOrderByCreatedAtDesc(Long userId, LocalDate today);
}