package com.duck.moodflix.movie.dto.tmdb.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * TMDb 영화 키워드 응답 DTO
 * /movie/{id}?append_to_response=keywords 응답의
 * "keywords": { "keywords": [ { "id": 123, "name": "..." }, ... ] }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KeywordsResponseDto(
        @JsonProperty("keywords") List<KeywordDto> keywords
) {}
