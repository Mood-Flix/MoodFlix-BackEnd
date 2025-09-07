package com.duck.moodflix.movie.service;

import com.duck.moodflix.movie.domain.entity.Keyword;
import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.domain.entity.MovieKeyword;
import com.duck.moodflix.movie.repository.KeywordRepository;
import com.duck.moodflix.movie.repository.MovieKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class KeywordManager {

    private final KeywordRepository keywordRepository;
    private final MovieKeywordRepository movieKeywordRepository;

    @Transactional
    public void upsert(Movie movie, List<String> names) {
        if (names == null || names.isEmpty()) return;

        // 1) 입력 정규화 (trim + lower), 최초 원형 보존
        Map<String, String> canonical = new LinkedHashMap<>();
        for (String raw : names) {
            if (raw == null) continue;
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) continue;
            String lower = trimmed.toLowerCase(Locale.ROOT);
            canonical.putIfAbsent(lower, trimmed);
        }
        if (canonical.isEmpty()) return;

        // 2) 벌크 조회 (lower(name) IN ...)
        Map<String, Keyword> found = new HashMap<>();
        keywordRepository.findByNameLowerIn(canonical.keySet())
                .forEach(k -> found.put(k.getName().toLowerCase(Locale.ROOT), k));

        // 3) 영화에 이미 연결된 키워드 ID를 한 번에 로딩
        Set<Long> existingKeywordIds = movieKeywordRepository.findKeywordIdsByMovieId(movie.getId());

        // 4) 업서트 + 조인 업서트
        for (Map.Entry<String, String> e : canonical.entrySet()) {
            String lower = e.getKey();
            String display = e.getValue();

            Keyword k = found.get(lower);
            if (k == null) {
                try {
                    k = keywordRepository.save(Keyword.builder().name(display).build());
                } catch (DataIntegrityViolationException ex) {
                    // 동시 생성 경쟁 시 재조회로 회복
                    k = keywordRepository.findByNameIgnoreCase(display).orElseThrow();
                }
                found.put(lower, k);
            }

            if (!existingKeywordIds.contains(k.getId())) {
                movieKeywordRepository.save(MovieKeyword.of(movie, k));
                existingKeywordIds.add(k.getId());
            }
        }
    }
}
