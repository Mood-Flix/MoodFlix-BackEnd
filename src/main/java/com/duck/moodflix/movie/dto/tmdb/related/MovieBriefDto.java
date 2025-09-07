package com.duck.moodflix.movie.dto.tmdb.related;
import com.fasterxml.jackson.annotation.JsonProperty;
public record MovieBriefDto(
        Long id,
        String title,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("release_date") String releaseDate
) {}
