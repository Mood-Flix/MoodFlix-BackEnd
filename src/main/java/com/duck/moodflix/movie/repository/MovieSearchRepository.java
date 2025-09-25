package com.duck.moodflix.movie.repository;

import com.duck.moodflix.movie.search.MovieDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieSearchRepository extends ElasticsearchRepository<MovieDoc, Long> {
    // ...
}
