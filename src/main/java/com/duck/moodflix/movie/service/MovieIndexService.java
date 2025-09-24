package com.duck.moodflix.movie.service;

import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.search.MovieDoc;
import com.duck.moodflix.movie.util.HangulUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieIndexService {

    private final ElasticsearchOperations esOps;

    @PersistenceContext
    private EntityManager em;

    /** 대량 색인: 안전하게 배치로 나눠 저장 + 저장 직후 refresh */
    public void indexMovies(List<Movie> movies) {
        if (movies == null || movies.isEmpty()) return;

        final int BATCH = 500; // 필요 시 500~1000 사이로 조절
        IndexOperations io = esOps.indexOps(MovieDoc.class);

        int from = 0, total = 0;
        while (from < movies.size()) {
            int to = Math.min(from + BATCH, movies.size());

            // 1) 이번 배치의 movieId 수집
            List<Long> ids = new ArrayList<>(to - from);
            for (int i = from; i < to; i++) {
                Movie m = movies.get(i);
                if (m != null && m.getId() != null) {
                    ids.add(m.getId());
                }
            }

            // 2) 배치 키워드 일괄 조회: movieId -> List<String>
            Map<Long, List<String>> kwsMap = fetchKeywordNames(ids);

            // 3) 문서화
            List<MovieDoc> docs = new ArrayList<>(to - from);
            for (int i = from; i < to; i++) {
                Movie m = movies.get(i);
                if (m == null) continue;
                List<String> kws = kwsMap.getOrDefault(m.getId(), List.of());
                docs.add(toDoc(m, kws));
            }

            // 4) 색인 + refresh
            if (!docs.isEmpty()) {
                log.info("[ES] indexing batch: {} docs ({} ~ {})", docs.size(), from, to - 1);
                esOps.save(docs);
                io.refresh(); // 즉시 검색/카운트 반영
                total += docs.size();
            }

            from = to;
        }
        log.info("[ES] indexing done. total={}", total);
    }

    /** 단건 색인: 저장 직후 refresh */
    public void indexMovie(Movie m) {
        if (m == null) return;

        // 단건일 때도 키워드 쿼리로 안전하게 조회
        List<String> kws = (m.getId() == null)
                ? List.of()
                : fetchKeywordNames(List.of(m.getId())).getOrDefault(m.getId(), List.of());

        esOps.save(toDoc(m, kws));
        esOps.indexOps(MovieDoc.class).refresh();
    }

    /** 배치로 영화 키워드 이름 일괄 조회 (N+1 / LAZY 회피) */
    private Map<Long, List<String>> fetchKeywordNames(List<Long> movieIds) {
        if (movieIds == null || movieIds.isEmpty()) return Map.of();

        // JPQL: MovieKeyword(mk) → Keyword(k) 이름을 movieId별로 모음
        var q = em.createQuery("""
            select mk.movie.id, k.name
            from MovieKeyword mk
            join mk.keyword k
            where mk.movie.id in :ids
        """, Object[].class);
        q.setParameter("ids", movieIds);

        List<Object[]> rows = q.getResultList();
        return rows.stream().collect(Collectors.groupingBy(
                r -> (Long) r[0],
                Collectors.mapping(r -> {
                    String name = (String) r[1];
                    return (name == null) ? "" : name.trim();
                }, Collectors.filtering(s -> s != null && !s.isBlank(), Collectors.toList()))
        ));
    }

    private MovieDoc toDoc(Movie m, List<String> keywordNames) {
        String title = (m.getTitle() == null) ? "" : m.getTitle();
        String choseong = HangulUtils.toChoseongKey(title);

        List<String> kws = (keywordNames == null ? List.<String>of() : keywordNames).stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .limit(50)
                .toList();

        Double popularity = (m.getPopularity() == null) ? 0.0 : m.getPopularity();
        Double voteAvg    = (m.getVoteAverage() == null) ? 0.0 : m.getVoteAverage();

        return MovieDoc.builder()
                .id(m.getId())
                .tmdbId(m.getTmdbId())
                .title(title)
                .titleChoseong(choseong)
                .posterUrl(m.getPosterUrl())
                .genre(m.getGenre())
                .keywords(kws)
                .adult(m.isAdult())
                .releaseDate(m.getReleaseDate())
                .popularity(popularity)
                .voteAverage(voteAvg)
                .build();
    }
}
