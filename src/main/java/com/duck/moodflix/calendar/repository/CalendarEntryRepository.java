package com.duck.moodflix.calendar.repository;

import com.duck.moodflix.calendar.domain.entity.CalendarEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CalendarEntryRepository extends JpaRepository<CalendarEntry, Long> {

    /**
     * [N+1 최적화] LEFT JOIN FETCH로 movie 즉시 로딩
     */
    @Query("SELECT ce FROM CalendarEntry ce LEFT JOIN FETCH ce.movie WHERE ce.user.userId = :userId AND ce.date = :date")
    Optional<CalendarEntry> findByUser_UserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * [N+1 최적화] LEFT JOIN FETCH로 movie 즉시 로딩 (월별)
     */
    @Query("SELECT ce FROM CalendarEntry ce LEFT JOIN FETCH ce.movie WHERE ce.user.userId = :userId AND ce.date BETWEEN :startDate AND :endDate")
    List<CalendarEntry> findByUser_UserIdAndDateBetween(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * [중복 방지] 최신 엔트리 조회
     */
    @Query("SELECT ce FROM CalendarEntry ce LEFT JOIN FETCH ce.movie WHERE ce.user.userId = :userId AND ce.date = :date ORDER BY ce.createdAt DESC")
    Optional<CalendarEntry> findFirstByUserUserIdAndDateOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT ce FROM CalendarEntry ce " +
            "LEFT JOIN FETCH ce.movie " +
            "LEFT JOIN FETCH ce.recommendation r " + // recommendation 필드명으로 가정
            "LEFT JOIN FETCH r.userEmotionInput " + // recommendation의 userEmotionInput 필드명으로 가정
            "WHERE ce.shareUuid = :shareUuid")
    Optional<CalendarEntry> findByShareUuid(@Param("shareUuid") String shareUuid);
}