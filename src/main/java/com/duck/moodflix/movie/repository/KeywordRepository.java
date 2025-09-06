package com.duck.moodflix.movie.repository;

import com.duck.moodflix.movie.domain.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    Optional<Keyword> findByNameIgnoreCase(String name);
    List<Keyword> findByNameIn(Iterable<String> names);

    @Query("select k from Keyword k where lower(k.name) in :names")
    List<Keyword> findByNameLowerIn(@Param("names") Collection<String> names);

}
