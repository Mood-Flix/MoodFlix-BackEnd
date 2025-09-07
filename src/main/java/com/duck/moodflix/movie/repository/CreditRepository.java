package com.duck.moodflix.movie.repository;

import com.duck.moodflix.movie.domain.entity.Credit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditRepository extends JpaRepository<Credit, Long> {
    List<Credit> findByMovieId(Long movieId);
}