package com.duck.moodflix.calendar.domain.entity;

import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.recommend.domain.entity.Recommendation;
import com.duck.moodflix.users.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_entries")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder // 클래스 레벨에 @Builder 적용
public class CalendarEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = true) // nullable로 변경
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id")
    private Recommendation recommendation;

    @Column(nullable = false)
    private LocalDate date;

    @Column(columnDefinition = "TEXT")
    private String userInputText;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(length = 10)
    private String moodEmoji;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 기존 빌더 생성자 제거 (클래스 레벨 @Builder로 대체)

    // note와 moodEmoji 업데이트 메서드
    public void updateNoteAndMood(String note, String moodEmoji) {
        this.note = note;
        this.moodEmoji = moodEmoji;
        this.updatedAt = LocalDateTime.now();
    }
}