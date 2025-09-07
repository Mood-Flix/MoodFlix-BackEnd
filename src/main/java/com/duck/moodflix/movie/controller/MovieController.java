package com.duck.moodflix.movie.controller;

import com.duck.moodflix.movie.dto.response.MovieDetailResponse;
import com.duck.moodflix.movie.dto.response.MovieSummaryResponse;
import com.duck.moodflix.movie.service.MovieQueryService;
import com.duck.moodflix.movie.service.MovieSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Movie API", description = "영화 정보 관리 API")
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieSyncService syncService;
    private final MovieQueryService queryService;

    @Operation(
            summary = "TMDb 영화 정보 동기화",
            description = "TMDb에서 인기 영화(1페이지)를 가져와 DB에 저장합니다. (관리자 권한 권장)"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sync")
    public ResponseEntity<String> syncMovies() {
        syncService.syncPopularPage1();
        return ResponseEntity.ok("Movie data synchronization completed.");
    }

    @Operation(summary = "전체 영화 목록(요약) 조회")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    public ResponseEntity<List<MovieSummaryResponse>> getAllMovies() {
        return ResponseEntity.ok(queryService.getAllMovieSummariesDto());
    }

    @Operation(summary = "영화 상세 조회")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{id}")
    public ResponseEntity<MovieDetailResponse> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getMovieDetailResponse(id));
    }
}
