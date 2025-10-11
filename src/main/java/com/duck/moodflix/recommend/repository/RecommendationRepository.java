package com.duck.moodflix.recommend.repository;


import com.duck.moodflix.recommend.domain.entity.Recommendation;
import com.duck.moodflix.recommend.domain.entity.UserEmotionInput;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    // 입력 1건에 대한 Top-N
    List<Recommendation> findByUserEmotionInputOrderBySimilarityScoreDesc(UserEmotionInput input);

    // 사용자별 최근 기록 조회
    // Page<Recommendation> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}