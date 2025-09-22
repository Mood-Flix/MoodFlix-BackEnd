package com.duck.moodflix.movie.controller;

import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.dto.response.MovieDetailResponse;
import com.duck.moodflix.movie.dto.response.MovieSummaryResponse;
import com.duck.moodflix.movie.repository.MovieRepository;
import com.duck.moodflix.movie.search.MovieDoc;
import com.duck.moodflix.movie.service.MovieQueryService;
import com.duck.moodflix.movie.service.MovieSearchService;
import com.duck.moodflix.movie.service.MovieSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
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
    private final MovieRepository movieRepository;
    private final MovieSearchService movieSearchService;

    @Operation(
            summary = "TMDb 영화 정보 동기화",
            description = "TMDb의 인기 영화 **모든 페이지**를 순회하여 DB에 저장합니다. (ADMIN 역할만 접근 가능)"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/sync")
    public ResponseEntity<String> syncMovies() {
        int saved = syncService.syncAllPopular(); // ✅ 모든 페이지 동기화
        return ResponseEntity.ok("Movie data synchronization completed. saved=" + saved);
    }

    @Operation(summary = "TMDb Discover 동기화(연도 범위)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/sync/discover")
    public ResponseEntity<String> syncDiscover(
            @RequestParam int fromYear,
            @RequestParam int toYear) {
        int saved = syncService.syncDiscoverByYearRange(fromYear, toYear);
        return ResponseEntity.ok("Discover sync done. saved=" + saved);
    }


    @Operation(summary = "전체 영화 목록(요약) 조회 - 페이징")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    public ResponseEntity<Page<MovieSummaryResponse>> getAllMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeAdult) {

        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size)); // 사이즈 가드 (1~100)
        Pageable pageable = PageRequest.of(
                p, s
        );

        Page<MovieSummaryResponse> result = queryService.getMovieSummaries(pageable, includeAdult);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "영화 상세 조회")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{id}")
    public ResponseEntity<MovieDetailResponse> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getMovieDetailResponse(id));
    }

    // 자동완성(글자 칠 때마다)
    @Operation(summary = "자동 완성", description = "글자를 칠 때마다 자동완성하는 기능")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/suggest")
    public ResponseEntity<List<MovieDoc>> suggest(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(movieSearchService.suggest(q, limit));
    }

    @Operation(summary = "영화 검색", description = "제목/키워드/장르로 전체 텍스트 검색")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/search")
    public ResponseEntity<Page<MovieDoc>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(movieSearchService.search(q, pageable));
    }

}
