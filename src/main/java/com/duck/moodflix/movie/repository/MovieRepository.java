package com.duck.moodflix.movie.repository;

import com.duck.moodflix.movie.domain.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    /**
     * TMDb의 고유 ID를 기준으로 영화를 조회합니다.
     * @param tmdbId TMDb에서 사용하는 영화의 고유 ID
     * @return Optional<Movie> 조회된 영화. 존재하지 않을 경우 Optional.empty() 반환
     */
    Optional<Movie> findByTmdbId(Long tmdbId);

    boolean existsByTmdbId(Long tmdbId);
}
