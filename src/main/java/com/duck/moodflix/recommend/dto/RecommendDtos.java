package com.duck.moodflix.recommend.dto;

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

    @Schema(name = "RecommendItem", description = "추천 결과 1건")
    public record Item(
            @Schema(description = "영화 ID", example = "1824")
            Long movieId,
            @Schema(description = "영화 제목", example = "에브리띵 윌 비 파인")
            String title,
            @Schema(description = "장르 목록", example = "[\"드라마\"]")
            List<String> genres,
            @Schema(description = "유사도(0~1)", example = "0.9368")
            double similarity
    ) {}

    @Schema(name = "RecommendResponse", description = "추천 응답")
    public record Response(
            @Schema(description = "모델 버전", example = "searle-j/kote_for_easygoing_people")
            String version,
            List<Item> items,
            @Schema(description = "입력 로그 ID", example = "456")
            Long logId
    ) {}
}
