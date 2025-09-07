package com.duck.moodflix.movie.dto.tmdb.credits;
import com.fasterxml.jackson.annotation.JsonProperty;
public record CrewDto(
        Long id,
        String name,
        String job,
        String department,
        @JsonProperty("profile_path") String profilePath
) {}