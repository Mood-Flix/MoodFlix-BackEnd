package com.duck.moodflix.movie.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "movie_details")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = "movie")
public class MovieDetails {

    /** 공유 PK: movies.id 와 동일 */
    @Id
    @Column(name = "movie_id")
    private Long id;

    /** Movie 와 1:1, Movie의 PK를 그대로 사용(@MapsId) */
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "movie_id")
    private Movie movie;

    /** 원제 */
    @Column(length = 255)
    private String originalTitle;

    /** 상태 (Released/Planned 등) */
    @Column(length = 32)
    private String status;

    /** 상영시간(분) */
    private Integer runtime;

    /** 대표 국가 코드/명 (production_countries[0]) */
    @Column(length = 2)
    private String countryCode;

    @Column(length = 64)
    private String countryName;

    /** 관람등급 (KR certification: 전체/12/15/18 등) */
    @Column(length = 8)
    private String certification;

    /** 상세 평점/지표 */
    private Integer voteCount;
    private Double  popularity;

    /** 예산/수익 */
    private Long budget;
    private Long revenue;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
