package com.duck.moodflix.movie.controller;

import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.dto.PageDto;
import com.duck.moodflix.movie.dto.response.MovieDetailResponse;
import com.duck.moodflix.movie.dto.response.MovieSummaryResponse;
import com.duck.moodflix.movie.repository.MovieRepository;
import com.duck.moodflix.movie.search.MovieDoc;
import com.duck.moodflix.movie.service.MovieIndexService;
import com.duck.moodflix.movie.service.MovieQueryService;
import com.duck.moodflix.movie.service.MovieSearchService;
import com.duck.moodflix.movie.service.MovieSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.misc.LogManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
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
    private final ElasticsearchOperations esOps;
    private final MovieIndexService movieIndexService;

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

    @Operation(summary = "영화 검색", description = "제목/키워드/장르로 전체 텍스트 검색")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/search")
    public ResponseEntity<PageDto<MovieDoc>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<MovieDoc> resultPage = movieSearchService.search(q, pageable);
        return ResponseEntity.ok(PageDto.from(resultPage));
    }

    // 이미 DB에 들어있는 영화 색인 용도
    @PostMapping("/reindex")
    public String reindex() {
        int page = 0, batch = 500, total = 0;
        while (true) {
            var p = movieRepository.findAll(org.springframework.data.domain.PageRequest.of(page, batch));
            if (!p.hasContent()) break;
            movieIndexService.indexMovies(p.getContent());
            total += p.getNumberOfElements();
            page++;
        }
        return "reindexed: " + total;
    }


}
