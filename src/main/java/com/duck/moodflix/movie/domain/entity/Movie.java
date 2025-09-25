package com.duck.moodflix.movie.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "movies",
        indexes = {
                @Index(name = "idx_movies_tmdb_id", columnList = "tmdb_id", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Movie {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_id", nullable = false, unique = true)
    private Long tmdbId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String overview;

    private String posterUrl;

    private LocalDate releaseDate;

    private String genre;

    private Double voteAverage;

    @Column(name = "adult", nullable = false)
    private boolean adult;

    @Column(name = "popularity")
    private Double popularity;

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
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
