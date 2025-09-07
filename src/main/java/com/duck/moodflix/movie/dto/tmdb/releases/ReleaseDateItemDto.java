package com.duck.moodflix.movie.dto.tmdb.releases;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReleaseDateItemDto {
    private String certification;
    private Integer type;

    @JsonProperty("release_date")
    private String releaseDate;
}
