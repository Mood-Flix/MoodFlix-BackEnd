package com.duck.moodflix.movie.dto.tmdb.releases;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReleaseDatesResultDto {
    @JsonProperty("iso_3166_1")
    private String iso31661;

    @JsonProperty("release_dates")
    private List<ReleaseDateItemDto> releaseDates;
}
