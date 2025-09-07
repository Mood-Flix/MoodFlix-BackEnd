package com.duck.moodflix.movie.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "영화 요약 응답 (리스트/카드용)")
public record MovieSummaryResponse(
        @Schema(description="DB PK") Long id,
        @Schema(description="TMDb ID", example="550") Long tmdbId,
        @Schema(description="제목", example="기생충") String title,
        @Schema(description="포스터 URL") String posterUrl,
        @Schema(description="대표 장르", example="Drama") String genre,
        @Schema(description="개봉일") LocalDate releaseDate,
        @Schema(description="평균 평점(0~10)", example="8.6") Double voteAverage
) {}
