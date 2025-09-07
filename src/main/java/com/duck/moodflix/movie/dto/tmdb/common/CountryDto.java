package com.duck.moodflix.movie.dto.tmdb.common;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CountryDto(
        @JsonProperty("iso_3166_1") String iso31661,
        String name
) {}
