package com.duck.moodflix.movie.service;

import com.duck.moodflix.movie.domain.entity.Keyword;
import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.domain.entity.MovieKeyword;
import com.duck.moodflix.movie.repository.KeywordRepository;
import com.duck.moodflix.movie.repository.MovieKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
public class KeywordManager {

    private final KeywordRepository keywordRepository;
    private final MovieKeywordRepository movieKeywordRepository;

    @Transactional
    public void upsert(Movie movie, List<String> names) {
        if (names == null || names.isEmpty()) return;

        Map<String, String> canonical = new LinkedHashMap<>();
        for (String raw : names) {
            if (raw == null) continue;
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) continue;
            String lower = trimmed.toLowerCase(Locale.ROOT);
            canonical.putIfAbsent(lower, trimmed);
        }
        if (canonical.isEmpty()) return;

        Map<String, Keyword> found = new HashMap<>();
        keywordRepository.findByNameLowerIn(canonical.keySet())
                .forEach(k -> found.put(k.getName().toLowerCase(Locale.ROOT), k));

        for (Map.Entry<String, String> e : canonical.entrySet()) {
            String lower = e.getKey();
            String display = e.getValue();

            Keyword k = found.get(lower);
            if (k == null) {
                k = keywordRepository.save(Keyword.builder().name(display).build());
                found.put(lower, k);
            }

            if (!movieKeywordRepository.existsByMovieIdAndKeywordId(movie.getId(), k.getId())) {
                movieKeywordRepository.save(MovieKeyword.of(movie, k));
            }
        }
    }
}
