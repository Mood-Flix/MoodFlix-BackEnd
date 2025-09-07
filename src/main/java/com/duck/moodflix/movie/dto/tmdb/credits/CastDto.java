package com.duck.moodflix.movie.dto.tmdb.credits;
import com.fasterxml.jackson.annotation.JsonProperty;
public record CastDto(
        Long id,
        String name,
        String character,
        Integer order,
        @JsonProperty("profile_path") String profilePath
) {}