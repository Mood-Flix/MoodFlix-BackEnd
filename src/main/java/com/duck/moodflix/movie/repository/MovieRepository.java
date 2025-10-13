package com.duck.moodflix.movie.repository;

import com.duck.moodflix.movie.domain.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    Page<Movie> findByAdultFalse(Pageable pageable);


    @Query(
            value = """
            select distinct m
            from Movie m
            left join m.movieKeywords mk
            left join mk.keyword k
            left join Credit c on c.movie = m
            left join c.person p
            where
              (:includeAdult = true or m.adult = false)
              and (
                  :q is null
                  or TRIM(:q) = ''
                  or lower(m.title) like lower(concat('%', :q, '%'))
                  or lower(m.genre) like lower(concat('%', :q, '%'))
                  or lower(k.name) like lower(concat('%', :q, '%'))
                  or lower(p.name) like lower(concat('%', :q, '%'))
              )
        """,
            countQuery = """
            select count(distinct m.id)
            from Movie m
            left join m.movieKeywords mk
            left join mk.keyword k
            left join Credit c on c.movie = m
            left join c.person p
            where
              (:includeAdult = true or m.adult = false)
              and (
                  :q is null
                  or TRIM(:q) = ''
                  or lower(m.title) like lower(concat('%', :q, '%'))
                  or lower(m.genre) like lower(concat('%', :q, '%'))
                  or lower(k.name) like lower(concat('%', :q, '%'))
                  or lower(p.name) like lower(concat('%', :q, '%'))
              )
        """
    )
    Page<Movie> searchByText(@Param("q") String q,
                             @Param("includeAdult") boolean includeAdult,
                             Pageable pageable);

    List<Movie> findByIdIn(List<Long> movieIds);
}
