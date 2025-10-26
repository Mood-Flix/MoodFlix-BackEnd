package com.duck.moodflix.calendar.service;

import com.duck.moodflix.calendar.domain.entity.CalendarEntry;
import com.duck.moodflix.calendar.dto.CalendarDtos;
import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.dto.response.MovieSummaryResponse;
import com.duck.moodflix.movie.repository.MovieRepository;
import com.duck.moodflix.recommend.domain.entity.Recommendation;
import com.duck.moodflix.recommend.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CalendarMapper {

    private final RecommendationRepository recommendationRepository;
    private final MovieRepository movieRepository;
    private static final int MAX_RECOMMENDATIONS = 5;

    public CalendarDtos.EntryResponse toEntryResponse(CalendarEntry entry) {
        List<CalendarDtos.RecommendationResponse> recommendationResponses = getRecommendationResponsesBlocking(
                entry.getUser().getUserId(), entry.getDate());

        MovieSummaryResponse selectedMovieResponse = null;
        if (entry.getMovie() != null) {
            Movie movie = entry.getMovie();
            selectedMovieResponse = new MovieSummaryResponse(
                    movie.getId(), movie.getTmdbId(), movie.getTitle(), movie.getPosterUrl(),
                    movie.getGenre(), movie.getReleaseDate(), movie.getVoteAverage()
            );
        }

        String posterUrlToUse = entry.getPosterUrl() != null ? entry.getPosterUrl() :
                (selectedMovieResponse != null ? selectedMovieResponse.posterUrl() : null);

        return new CalendarDtos.EntryResponse(
                entry.getShareUuid(), // shareUuid 사용
                entry.getDate(),
                entry.getNote(),
                entry.getMoodEmoji(),
                selectedMovieResponse,
                recommendationResponses,
                posterUrlToUse // selectedMovie.posterUrl로 대체 가능성 제공
        );
    }

    /**
     * [수정] 당일 최신 추천 5개만 반환 (created_at DESC 정렬)
     */
    private List<CalendarDtos.RecommendationResponse> getRecommendationResponsesBlocking(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59, 999999999);

        Pageable pageable = PageRequest.of(0, MAX_RECOMMENDATIONS, Sort.by("createdAt").descending());

        List<Recommendation> recommendations = recommendationRepository.findByUserUserIdAndCreatedAtBetween(
                userId, startOfDay, endOfDay, pageable);

        if (recommendations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> movieIds = recommendations.stream()
                .map(Recommendation::getMovieId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Movie> movieMap = movieRepository.findAllById(movieIds).stream()
                .collect(Collectors.toMap(Movie::getId, Function.identity()));

        return recommendations.stream()
                .map(reco -> {
                    Movie movie = movieMap.get(reco.getMovieId());
                    return new CalendarDtos.RecommendationResponse(
                            reco.getId(),
                            reco.getMovieId(),
                            movie != null ? movie.getTitle() : "Unknown",
                            reco.getSimilarityScore(),
                            reco.getUserEmotionInput() != null ? reco.getUserEmotionInput().getInputText() : "Unknown"
                    );
                })
                .collect(Collectors.toList());
    }

    public CalendarDtos.EntryResponse createEmptyEntryResponse(Long userId, LocalDate date) {
        List<CalendarDtos.RecommendationResponse> recommendationResponses =
                getRecommendationResponsesBlocking(userId, date);
        return new CalendarDtos.EntryResponse(null, date, null, null, null, recommendationResponses, null);
    }
}