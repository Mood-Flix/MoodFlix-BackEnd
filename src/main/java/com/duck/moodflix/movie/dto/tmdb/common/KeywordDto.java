package com.duck.moodflix.movie.dto.tmdb.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TMDb 키워드 항목 DTO
 * 예) { "id": 123, "name": "cybersecurity" }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KeywordDto(
        @JsonProperty("id") Long id,
        @JsonProperty("name") String name
) {}
