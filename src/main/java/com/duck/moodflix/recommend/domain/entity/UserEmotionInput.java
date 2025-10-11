package com.duck.moodflix.recommend.domain.entity;

import com.duck.moodflix.users.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_emotion_inputs",
        indexes = {
                @Index(name = "idx_user_emotion_inputs_user", columnList = "user_id"),
                @Index(name = "idx_user_emotion_inputs_created_at", columnList = "createdAt")
        })
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class UserEmotionInput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 비로그인 허용 시 optional=true 로 두고 DB에서 user_id nullable 허용
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String inputText;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
