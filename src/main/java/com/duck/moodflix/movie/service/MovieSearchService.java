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

        // 1) 제목 정확 일치 먼저 시도 (정규화 + exact 필드)
        String normForExact = normalizeForExact(raw);          // 공백/콜론/유니코드 정리 + 소문자
        Page<MovieDoc> exact = tryExactTitle(normForExact);
        if (exact != null) return exact;                      // 정확히 한 건만 반환

        // 2) 폴백: 초성/일반 검색 (기존 우선순위)
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

        // 3) 마지막 안전장치: 결과 중 ‘제목 완전 동일’이 있으면 그 1건만 반환
        String normOrig = normalizeForExact(raw);
        List<MovieDoc> exactInResults = hits.stream()
                .map(SearchHit::getContent)
                .filter(d -> normalizeForExact(d.getTitle()).equals(normOrig))
                .sorted((a,b) -> Double.compare(
                        b.getVoteAverage() == null ? 0.0 : b.getVoteAverage(),
                        a.getVoteAverage() == null ? 0.0 : a.getVoteAverage()))
                .limit(1)
                .toList();
        if (!exactInResults.isEmpty()) {
            return new PageImpl<>(exactInResults, PageRequest.of(0, 1), 1);
        }

        return toPage(hits, pageable);
    }

    private Page<MovieDoc> tryExactTitle(String normalizedLower) {
        // 1) 정확 일치만 시도
        NativeQuery termExact = NativeQuery.builder()
                .withQuery(qb -> qb.term(t -> t.field("title.exact").value(normalizedLower)))
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
        String t = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKC);
        // 콜론/하이픈 등 흔한 변형 통일
        t = t.replace('：', ':').replace('‐','-').replace('–','-').replace('—','-');
        // 여러 공백 → 하나, 앞뒤 trim
        t = t.replaceAll("\\s+", " ").trim();
        return t.toLowerCase(); // title.exact에 lowercase normalizer 가정
    }

    private Page<MovieDoc> toPage(SearchHits<MovieDoc> hits, Pageable pageable) {
        List<MovieDoc> content = hits.stream().map(SearchHit::getContent).toList();
        long total = hits.getTotalHits();
        return new PageImpl<>(content, pageable, total);
    }
}
