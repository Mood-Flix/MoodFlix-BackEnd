package com.duck.moodflix.movie.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long tmdbId;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(length = 255)
    private String posterUrl;

    @Column(length = 100)
    private String genre;

    private LocalDate releaseDate;

    private Double voteAverage;

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<MovieKeyword> movieKeywords = new LinkedHashSet<>();

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

    /** 관계 해제: orphanRemoval=true 에 맡김 */
    public void removeMovieKeyword(MovieKeyword mk) {
        this.movieKeywords.remove(mk);
        // mk.setMovie(null);  // nullable=false이므로 null 세팅하지 않음
    }
}
