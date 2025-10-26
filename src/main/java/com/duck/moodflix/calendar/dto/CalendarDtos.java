package com.duck.moodflix.calendar.dto;

import com.duck.moodflix.movie.dto.response.MovieSummaryResponse;

import java.time.LocalDate;
import java.util.List;

public class CalendarDtos {

    public record EntryRequest(
            LocalDate date,      // 캘린더 날짜
            String moodEmoji,    // 기분 이모지
            String note,          // 사용자 메모
            Long movieId          //[프론트 추천 선택] 영화 ID (선택)
    ) {}

    public record RecommendationResponse(
            Long recommendationId,    // 추천 ID
            Long movieId,            // 영화 ID
            String movieTitle,       // 영화 제목 (Movie 엔티티에서)
            double similarityScore,   // 유사도 점수
            String userInputText     // 사용자 입력 문장 (UserEmotionInput에서)
    ) {}

    public record EntryResponse(
            String id,
            LocalDate date,
            String note,
            String moodEmoji,
            MovieSummaryResponse selectedMovie, // 선택된 영화 정보
            List<RecommendationResponse> recommendations,
            String posterUrl
    ) {}
}