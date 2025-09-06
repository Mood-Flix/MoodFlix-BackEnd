package com.duck.moodflix.movie.repository;

import com.duck.moodflix.movie.domain.entity.TmdbReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface TmdbReviewRepository extends JpaRepository<TmdbReview, String> {
    List<TmdbReview> findByMovie_Id(Long movieId, Pageable pageable);
    List<TmdbReview> findTop5ByMovie_IdOrderByCreatedAtTmdbDesc(Long movieId);

    boolean existsById(String id);

}

