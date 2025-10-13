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

@Service
@RequiredArgsConstructor
public class MovieSearchService {

    private final ElasticsearchOperations esOps;
    private static final String EXACT_FIELD = "title.exact";

    @Transactional(readOnly = true)
    public Page<MovieDoc> search(String q, Pageable pageable) {
        String raw = (q == null) ? "" : q.trim();
        if (raw.isEmpty()) {
            NativeQuery nq = NativeQuery.builder()
                    .withQuery(qb -> qb.matchAll(m -> m))
                    .withPageable(pageable)
                    .withSort(Sort.by(
                            Sort.Order.desc("_score"),
                            Sort.Order.desc("voteAverage"),
                            Sort.Order.desc("tmdbId")
                    ))
                    .build();
            return toPage(esOps.search(nq, MovieDoc.class), pageable);
        }

        Page<MovieDoc> exact = tryExactTitle(raw);
        if (exact != null) return exact;

        boolean isCho = HangulUtils.isChoseongQuery(raw);
        NativeQuery nq = isCho
                ? NativeQuery.builder()
                .withQuery(qb -> qb.bool(b -> b
                        .should(s -> s.prefix(p -> p.field("titleChoseong.raw").value(raw)))
                        .should(s -> s.match(mq -> mq.field("titleChoseong.ngram").query(raw)))
                ))
                .withPageable(pageable)
                .withSort(Sort.by(
                        Sort.Order.desc("_score"),
                        Sort.Order.desc("voteAverage"),
                        Sort.Order.desc("tmdbId")
                ))
                .build()
                : NativeQuery.builder()
                .withQuery(qb -> qb.bool(b -> b
                        .must(m -> m.multiMatch(mm -> mm
                                .query(raw)
                                .type(BestFields)
                                .fields("title^5", "keywords^2", "genre")
                        ))
                ))
                .withPageable(pageable)
                .withSort(Sort.by(
                        Sort.Order.desc("_score"),
                        Sort.Order.desc("voteAverage"),
                        Sort.Order.desc("tmdbId")
                ))
                .build();

        SearchHits<MovieDoc> hits = esOps.search(nq, MovieDoc.class);

        String normOrig = normalizeForExact(raw);
        List<MovieDoc> exactInResults = hits.stream()
                .map(SearchHit::getContent)
                .filter(d -> normalizeForExact(d.getTitle()).equals(normOrig))
                .sorted((a, b) -> Double.compare(
                        b.getVoteAverage() == null ? 0.0 : b.getVoteAverage(),
                        a.getVoteAverage() == null ? 0.0 : a.getVoteAverage()))
                .limit(1)
                .toList();
        if (!exactInResults.isEmpty()) {
            return new PageImpl<>(exactInResults, PageRequest.of(0, 1), 1);
        }

        return toPage(hits, pageable);
    }

    private Page<MovieDoc> tryExactTitle(String rawTitle) {
        String normalized = normalizeForExact(rawTitle);
        NativeQuery termExact = NativeQuery.builder()
                .withQuery(qb -> qb.term(t -> t.field(EXACT_FIELD).value(normalized)))
                .withPageable(PageRequest.of(0, 1))
                .withSort(Sort.by(Sort.Order.desc("voteAverage"), Sort.Order.desc("tmdbId")))
                .build();
        SearchHits<MovieDoc> termHits = esOps.search(termExact, MovieDoc.class);
        if (termHits.getTotalHits() > 0) {
            var one = termHits.stream().map(SearchHit::getContent).limit(1).toList();
            return new PageImpl<>(one, PageRequest.of(0, 1), 1);
        }
        return null;
    }

    private String normalizeForExact(String s) {
        if (s == null) return "";
        // ES normalizer(title.exact)와 동일 규칙 유지:
        // NFKC → 전각 콜론/대시 통일 → 다중 공백 축소/trim → lowercase
        String t = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKC);
        t = t.replace('：', ':')
                .replace('‐','-')
                .replace('–','-')
                .replace('—','-');
        t = t.replaceAll("\\s+", " ").trim();
        return t.toLowerCase();
    }

    private Page<MovieDoc> toPage(SearchHits<MovieDoc> hits, Pageable pageable) {
        List<MovieDoc> content = hits.stream().map(SearchHit::getContent).toList();
        long total = hits.getTotalHits();
        return new PageImpl<>(content, pageable, total);
    }
}
