package com.duck.moodflix.movie.dto.tmdb.media;
import com.fasterxml.jackson.annotation.JsonProperty;
public record ImageItemDto(
        @JsonProperty("file_path") String filePath,
        Integer width,
        Integer height,
        @JsonProperty("aspect_ratio") Double aspectRatio
) {}
