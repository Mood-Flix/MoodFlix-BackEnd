package com.duck.moodflix.movie.repository;

import com.duck.moodflix.movie.domain.entity.MovieDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieDetailsRepository extends JpaRepository<MovieDetails, Long> {
}
