package com.duck.moodflix.users.domain.entity;

import com.duck.moodflix.emotion.domain.entity.EmotionTag;
import com.duck.moodflix.recommend.domain.entity.UserEmotionInput;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_emotion_scores",
        uniqueConstraints = @UniqueConstraint(columnNames = {"emotion_input_id", "tag_id"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEmotionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private EmotionTag tag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emotion_input_id", nullable = false)
    private UserEmotionInput emotionInput;

    // score는 0.0~1.0 범위로, null 불가
    @Column(nullable = false)
    @DecimalMin(value = "0.0", inclusive = true, message = "감정 백터 점수는 0.0 이상이어야 합니다")
    @DecimalMax(value = "1.0", inclusive = true, message = "감정 백터 점수는 1.0 이하여야 합니다")
    private Float score;

    // 점수 기록 시점 자동 관리
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

