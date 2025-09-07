package com.duck.moodflix.movie.dto.tmdb.releases;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReleaseDatesDto {
    private List<ReleaseDatesResultDto> results;
}
