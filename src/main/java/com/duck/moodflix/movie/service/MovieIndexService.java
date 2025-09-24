package com.duck.moodflix.movie.service;

import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.domain.entity.MovieKeyword;
import com.duck.moodflix.movie.search.MovieDoc;
import com.duck.moodflix.movie.util.HangulUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieIndexService {

    private final ElasticsearchOperations esOps;

    /** 대량 색인: 안전하게 배치로 나눠 저장 + 저장 직후 refresh */
    public void indexMovies(List<Movie> movies) {
        if (movies == null || movies.isEmpty()) return;

        final int BATCH = 500; // 필요 시 500~1000 사이로 조절
        IndexOperations io = esOps.indexOps(MovieDoc.class);

        int from = 0, total = 0;
        while (from < movies.size()) {
            int to = Math.min(from + BATCH, movies.size());
            List<MovieDoc> docs = new ArrayList<>(to - from);
            for (int i = from; i < to; i++) {
                Movie m = movies.get(i);
                if (m == null) continue;
                docs.add(toDoc(m));
            }

            if (!docs.isEmpty()) {
                log.info("[ES] indexing batch: {} docs ({} ~ {})", docs.size(), from, to - 1);
                esOps.save(docs);                 // 벌크 저장
                io.refresh();                     // ✅ 즉시 검색/카운트 반영
                total += docs.size();
            }
            from = to;
        }
        log.info("[ES] indexing done. total={}", total);
    }

    /** 단건 색인: 저장 직후 refresh */
    public void indexMovie(Movie m) {
        if (m == null) return;
        esOps.save(toDoc(m));
        esOps.indexOps(MovieDoc.class).refresh(); // ✅
    }

    private MovieDoc toDoc(Movie m) {
        String title = (m.getTitle() == null) ? "" : m.getTitle();
        String choseong = HangulUtils.toChoseongKey(title);

        List<String> kws = (m.getMovieKeywords() == null)
                ? List.of()
                : m.getMovieKeywords().stream()
                .map(MovieKeyword::getKeyword)
                .filter(k -> k != null && k.getName() != null && !k.getName().isBlank())
                .map(k -> k.getName().trim())
                .distinct()
                .limit(50)
                .collect(Collectors.toList());

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
