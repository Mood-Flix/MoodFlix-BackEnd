package com.duck.moodflix.movie.util;

import com.duck.moodflix.movie.dto.tmdb.TMDbMovieDetailDto;
import com.duck.moodflix.movie.dto.tmdb.releases.ReleaseDateItemDto;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.stream.Stream;

@Component
public class CertificationExtractor {

    public String extract(TMDbMovieDetailDto d) {
        if (d == null) return null; // ← null guard

        String c = forIso(d, "KR");
        if (c == null) c = forIso(d, "US");
        if (c == null && d.getReleaseDates() != null && d.getReleaseDates().getResults() != null) {
            return d.getReleaseDates().getResults().stream()
                    .flatMap(r -> r.getReleaseDates() == null
                            ? Stream.<ReleaseDateItemDto>empty()
                            : r.getReleaseDates().stream())
                    .map(ReleaseDateItemDto::getCertification)
                    .filter(s -> s != null && !s.isBlank())
                    .findFirst()
                    .orElse(null);
        }
        return c;
    }

    private String forIso(TMDbMovieDetailDto d, String iso2) {
        if (d.getReleaseDates() == null || d.getReleaseDates().getResults() == null) return null;
        return d.getReleaseDates().getResults().stream()
                .filter(r -> iso2.equalsIgnoreCase(r.getIso31661()))
                .findFirst()
                .flatMap(r -> (r.getReleaseDates() == null
                        ? Stream.<ReleaseDateItemDto>empty()
                        : r.getReleaseDates().stream())
                        .sorted(Comparator.comparingInt(x -> x.getType() == null ? 99 : x.getType()))
                        .map(ReleaseDateItemDto::getCertification)
                        .filter(s -> s != null && !s.isBlank())
                        .findFirst())
                .orElse(null);
    }
}
