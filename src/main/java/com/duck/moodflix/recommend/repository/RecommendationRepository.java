package com.duck.moodflix.recommend.repository;


import com.duck.moodflix.recommend.domain.entity.Recommendation;
import com.duck.moodflix.recommend.domain.entity.UserEmotionInput;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    // 입력 1건에 대한 Top-N
    List<Recommendation> findByUserEmotionInputOrderBySimilarityScoreDesc(UserEmotionInput input);

    List<Recommendation> findByUserEmotionInputId(Long id);

    // 사용자별 최근 기록 조회
    @Query("SELECT r FROM Recommendation r WHERE r.user.userId = :userId AND r.createdAt BETWEEN :start AND :end " +
            "ORDER BY r.createdAt DESC")
    List<Recommendation> findByUserUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);


    long countByUserUserIdAndCreatedAtBetween(Long userId, LocalDateTime startOfDay, LocalDateTime endOfDay);
}
