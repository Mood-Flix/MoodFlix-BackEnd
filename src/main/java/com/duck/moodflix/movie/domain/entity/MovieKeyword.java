package com.duck.moodflix.movie.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "movie_keywords",
        uniqueConstraints = @UniqueConstraint(name = "ux_movie_keyword", columnNames = {"movie_id", "keyword_id"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MovieKeyword {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    public static MovieKeyword of(Movie movie, Keyword keyword) {
        return MovieKeyword.builder().movie(movie).keyword(keyword).build();
    }
}
