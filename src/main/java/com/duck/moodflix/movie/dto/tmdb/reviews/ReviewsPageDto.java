package com.duck.moodflix.movie.dto.tmdb.reviews;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ReviewsPageDto(
        Integer page,
        java.util.List<ReviewDto> results,
        @JsonProperty("total_pages") Integer totalPages,
        @JsonProperty("total_results") Integer totalResults
) {}