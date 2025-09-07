package com.duck.moodflix.movie.dto.tmdb.reviews;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthorDetailsDto(
        String name,
        String username,
        @JsonProperty("avatar_path") String avatarPath,
        Double rating // 0~10 (TMDb 사용자 별점)
) {}