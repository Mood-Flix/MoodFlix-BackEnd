package com.duck.moodflix.calendar.repository;

import com.duck.moodflix.calendar.domain.entity.CalendarEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarEntryRepository extends JpaRepository<CalendarEntry, Long> {
    List<CalendarEntry> findByUserUserId(Long userId);
    Optional<CalendarEntry> findByIdAndUserUserId(Long id, Long userId);
    Optional<CalendarEntry> findByUser_UserIdAndDate(Long userId, LocalDate date);
    List<CalendarEntry> findByUserUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT c FROM CalendarEntry c LEFT JOIN FETCH c.userInputText WHERE c.user.userId = :userId AND c.date = :date")
    Optional<CalendarEntry> findByUserAndDateWithInputs(@Param("userId") Long userId, @Param("date") LocalDate date);

    Optional<CalendarEntry> findFirstByUserUserIdAndDateOrderByCreatedAtDesc(Long userId, LocalDate today);
}