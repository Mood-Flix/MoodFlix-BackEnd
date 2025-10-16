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
    // [수정] JOIN FETCH를 사용하여 Movie 엔티티를 함께 조회합니다.
    @Query("SELECT ce FROM CalendarEntry ce LEFT JOIN FETCH ce.movie WHERE ce.user.userId = :userId AND ce.date = :date")
    Optional<CalendarEntry> findByUser_UserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT ce FROM CalendarEntry ce LEFT JOIN FETCH ce.movie WHERE ce.user.userId = :userId AND ce.date BETWEEN :startDate AND :endDate")
    List<CalendarEntry> findByUser_UserIdAndDateBetween(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate")LocalDate endDate);

    @Query("SELECT c FROM CalendarEntry c LEFT JOIN FETCH c.userInputText WHERE c.user.userId = :userId AND c.date = :date")
    Optional<CalendarEntry> findByUserAndDateWithInputs(@Param("userId") Long userId, @Param("date") LocalDate date);

    Optional<CalendarEntry> findFirstByUserUserIdAndDateOrderByCreatedAtDesc(Long userId, LocalDate today);
}