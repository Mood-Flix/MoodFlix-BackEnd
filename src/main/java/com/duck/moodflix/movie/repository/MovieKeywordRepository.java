package com.duck.moodflix.movie.repository;

import com.duck.moodflix.movie.domain.entity.MovieKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface MovieKeywordRepository extends JpaRepository<MovieKeyword, Long> {

    boolean existsByMovieIdAndKeywordId(Long movieId, Long keywordId);

    @Query("select mk.keyword.id from MovieKeyword mk where mk.movie.id = :movieId")
    Set<Long> findKeywordIdsByMovieId(@Param("movieId") Long movieId);
}
