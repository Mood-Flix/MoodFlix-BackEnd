package com.duck.moodflix.calendar.domain.entity;

import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.recommend.domain.entity.Recommendation;
import com.duck.moodflix.users.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Entity
@Table(name = "calendar_entry",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_calendar_entry_user_date", // 제약 조건에 고유한 이름 부여
                        columnNames = {"user_id", "date"}   // 유니크해야 할 컬럼들의 조합
                )
        })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
@Builder // 클래스 레벨에 @Builder 적용
public class CalendarEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String shareUuid;

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
        if (this.shareUuid == null || this.shareUuid.trim().isEmpty()) {
            this.shareUuid = java.util.UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (this.shareUuid == null || this.shareUuid.toString().equals("00000000-0000-0000-0000-000000000000")) {
            this.shareUuid = String.valueOf(UUID.randomUUID());
            log.info("PreUpdate: Fixed shareUuid: {}", this.shareUuid);
        }
        this.updatedAt = LocalDateTime.now();
    }

    // 기존 빌더 생성자 제거 (클래스 레벨 @Builder로 대체)

    // note와 moodEmoji 업데이트 메서드
    public void updateNoteAndMood(String note, String moodEmoji) {
        this.note = note;
        this.moodEmoji = moodEmoji;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 추천을 생성한 사용자 입력 텍스트를 갱신합니다.
     * 이 메서드는 note나 moodEmoji 필드를 변경하지 않습니다.
     */
    public void updateUserInputText(String userInputText) {
        this.userInputText = userInputText;
    }

    public void updateMovie(Movie movie) {
        this.movie = movie;
    }
}