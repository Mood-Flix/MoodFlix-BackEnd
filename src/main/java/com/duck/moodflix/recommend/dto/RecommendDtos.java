package com.duck.moodflix.recommend.dto;

import com.duck.moodflix.movie.dto.response.MovieSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class RecommendDtos {

    @Schema(name = "RecommendRequest", description = "사용자 문장으로 추천 요청")
    public record Request(
            @Schema(description = "사용자 입력 문장", example = "나 너무 슬퍼")
            String text,
            @Schema(description = "추천 개수(기본 20)", example = "10")
            Integer topN
    ) {}

    @Schema(name = "RecommendItemResponse", description = "추천 결과 1건")
    public record RecommendItemResponse(
            @Schema(description = "영화 요약 정보")
            MovieSummaryResponse movie,
            @Schema(description = "유사도(0~1)", example = "0.9368")
            double similarity
    ) {}

    @Schema(name = "RecommendResponse", description = "추천 응답")
    public record Response(
            @Schema(description = "모델 버전", example = "searle-j/kote_for_easygoing_people")
            String version,
            @Schema(description = "추천 영화 목록")
            List<RecommendItemResponse> items,
            @Schema(description = "입력 로그 ID", example = "456")
            Long logId
    ) {}
}