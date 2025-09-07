package com.duck.moodflix.movie.dto.tmdb.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TMDb 장르 DTO (movie.genres[])
 * 예) { "id": 878, "name": "Science Fiction" }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GenreDto(
        @JsonProperty("id") Integer id,
        @JsonProperty("name") String name
) {}
