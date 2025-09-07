package com.duck.moodflix.movie.dto.tmdb.reviews;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ReviewDto(
        String id,
        String author,
        @JsonProperty("author_details") AuthorDetailsDto authorDetails,
        String content,
        @JsonProperty("created_at") String createdAt,
        String url
) {}