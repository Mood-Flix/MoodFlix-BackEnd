package com.duck.moodflix.movie.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@Entity
@Table(
        name = "tmdb_reviews",
        indexes = {
                @Index(name = "idx_tmdb_reviews_movie", columnList = "movie_id"),
                @Index(name = "idx_tmdb_reviews_created_at_tmdb", columnList = "created_at_tmdb")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TmdbReview {

    @Id
    private String id;

    private String author;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Double rating;
    private String url;

    @Column(name = "created_at_tmdb")
    private OffsetDateTime createdAtTmdb;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    public static TmdbReview of(
            String id, String author, String content, Double rating, String url, String createdAtIso, Movie movie) {

        TmdbReview r = new TmdbReview();
        r.id = id;
        r.author = author;
        r.content = content;
        r.rating = rating;
        r.url = url;
        try {
            r.createdAtTmdb = createdAtIso == null ? null : OffsetDateTime.parse(createdAtIso);
        } catch (DateTimeParseException e) {
            r.createdAtTmdb = null;
        }
        r.movie = movie;
        return r;
    }
}
