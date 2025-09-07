package com.duck.moodflix.movie.repository;

import com.duck.moodflix.movie.domain.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByTmdbId(Long tmdbId);
}
