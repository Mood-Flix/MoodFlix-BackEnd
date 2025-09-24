package com.duck.moodflix.movie.service;

import com.duck.moodflix.movie.search.MovieDoc;
import com.duck.moodflix.movie.util.HangulUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields;
import static co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BoolPrefix;

@Service
@RequiredArgsConstructor
public class MovieSearchService {

    private final ElasticsearchOperations esOps;

    @Transactional(readOnly = true)
    public Page<MovieDoc> search(String q, Pageable pageable) {
        String norm = (q == null) ? "" : q.trim();

        if (norm.isEmpty()) {
            NativeQuery nq = NativeQuery.builder()
                    .withQuery(qb -> qb.matchAll(m -> m))
                    .withPageable(pageable)
                    .withSort(Sort.by(Sort.Order.desc("voteAverage"), Sort.Order.desc("tmdbId")))
                    .build();
            SearchHits<MovieDoc> hits = esOps.search(nq, MovieDoc.class);
            return toPage(hits, pageable);
        }

        boolean isCho = HangulUtils.isChoseongQuery(norm);

        NativeQuery nq = isCho
                ? NativeQuery.builder()
                .withQuery(qb -> qb.bool(b -> b
                        .should(s -> s.prefix(p -> p.field("titleChoseong.raw").value(norm)))   // 빠른 접두
                        .should(s -> s.match(mq -> mq.field("titleChoseong.ngram").query(norm))) // 포함 매칭
                ))
                .withPageable(pageable)
                .withSort(Sort.by(Sort.Order.desc("voteAverage"), Sort.Order.desc("tmdbId")))
                .build()
                : NativeQuery.builder()
                .withQuery(qb -> qb.bool(b -> b
                        .must(m -> m.multiMatch(mm -> mm
                                .query(norm)
                                .type(BestFields)
                                .fields("title^5", "keywords^2", "genre")
                        ))
                ))
                .withPageable(pageable)
                .withSort(Sort.by(Sort.Order.desc("voteAverage"), Sort.Order.desc("tmdbId")))
                .build();

        return toPage(esOps.search(nq, MovieDoc.class), pageable);
    }


    private Page<MovieDoc> toPage(SearchHits<MovieDoc> hits, Pageable pageable) {
        List<MovieDoc> content = hits.stream().map(SearchHit::getContent).toList();
        long total = hits.getTotalHits();
        return new PageImpl<>(content, pageable, total);
    }
}
