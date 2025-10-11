package com.duck.moodflix.recommend.domain.entity;
import com.duck.moodflix.users.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendations",
        indexes = {
                @Index(name = "idx_reco_user_input", columnList = "user_id,user_emotion_input_id"),
                @Index(name = "idx_reco_input", columnList = "user_emotion_input_id")
        })
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 선택: user_id FK. 익명 허용 시 optional=true
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_emotion_input_id", nullable = false)
    private UserEmotionInput userEmotionInput;

    @Column(nullable = false)
    private Long movieId;

    @Column(nullable = false)
    private Double similarityScore;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

