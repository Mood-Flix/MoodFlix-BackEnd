package com.duck.moodflix.recommend.repository;

import com.duck.moodflix.recommend.domain.entity.UserEmotionInput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserEmotionInputRepository extends JpaRepository<UserEmotionInput, Long> {
}