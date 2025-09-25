package com.duck.moodflix.movie.service;

import com.duck.moodflix.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IndexHealthService {

    private final MovieRepository movieRepository;
    private final ElasticsearchOperations esOps;

    public long dbCount() { return movieRepository.count(); }

    public long esCount() {
        var q = org.springframework.data.elasticsearch.client.elc.NativeQuery.builder()
                .withQuery(qb -> qb.matchAll(m -> m))
                .build();
        return esOps.count(q, com.duck.moodflix.movie.search.MovieDoc.class);
    }

    /** ES가 살아있고 인덱스도 있는지 가볍게 확인 */
    public boolean esHealthy() {
        try {
            var io = esOps.indexOps(com.duck.moodflix.movie.search.MovieDoc.class);
            return io.exists();
        } catch (Exception e) {
            return false;
        }
    }

    /** 색인이 “필요한지”만 결정: ES 살아있고, ES < DB 일 때만 */
    public boolean shouldIndex() {
        if (!esHealthy()) return false;
        return esCount() < dbCount();
    }
}
