package com.duck.moodflix.movie.dto.tmdb;

import com.duck.moodflix.movie.dto.tmdb.related.MovieBriefDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * TMDb /movie/popular, /movie/top_rated 등 공통 리스트 응답 DTO
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TMDbMovieListResponse {

    @JsonProperty("page")
    private Integer page;

    @JsonProperty("results")
    private List<MovieBriefDto> results;

    @JsonProperty("total_pages")
    private Integer totalPages;

    @JsonProperty("total_results")
    private Integer totalResults;
}
