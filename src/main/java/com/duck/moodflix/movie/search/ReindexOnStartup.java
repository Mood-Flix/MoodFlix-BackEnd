package com.duck.moodflix.movie.search;

import com.duck.moodflix.movie.repository.MovieRepository;
import com.duck.moodflix.movie.service.IndexHealthService;
import com.duck.moodflix.movie.service.MovieIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReindexOnStartup {

    private final MovieRepository movieRepository;
    private final MovieIndexService movieIndexService;
    private final IndexHealthService health;

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public void reindexAfterStartup() {
        if (!health.shouldIndex()) {
            log.info("[Reindex] skip. dbCount={}, esCount={}", health.dbCount(), health.esCount());
            return;
        }


        final int batch = 500;
        int page = 0, total = 0;

        log.info("[Reindex] start");
        while (true) {
            var pageable = org.springframework.data.domain.PageRequest.of(page, batch);
            var p = movieRepository.findAll(pageable);
            if (!p.hasContent()) break;

            var list = p.getContent();
            log.info("[Reindex] page={} size={}", page, list.size());

            try {
                movieIndexService.indexMovies(list); // ES 벌크 색인
                log.info("[Reindex] indexed {} docs", list.size());
                total += list.size();
            } catch (Exception e) {
                log.error("[Reindex] indexing failed on page={}, err={}", page, e.toString(), e);
            }
            page++;
        }
        log.info("[Reindex] done. total indexed={}", total);
    }
}

