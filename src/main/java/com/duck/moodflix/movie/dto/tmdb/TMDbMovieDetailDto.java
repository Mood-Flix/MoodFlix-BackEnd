package com.duck.moodflix.movie.dto.tmdb;

import com.duck.moodflix.movie.dto.tmdb.common.CompanyDto;
import com.duck.moodflix.movie.dto.tmdb.common.CountryDto;
import com.duck.moodflix.movie.dto.tmdb.common.GenreDto;
import com.duck.moodflix.movie.dto.tmdb.common.KeywordsResponseDto;
import com.duck.moodflix.movie.dto.tmdb.credits.CreditsDto;
import com.duck.moodflix.movie.dto.tmdb.media.ImagesDto;
import com.duck.moodflix.movie.dto.tmdb.media.VideosDto;
import com.duck.moodflix.movie.dto.tmdb.related.MovieBriefPageDto;
import com.duck.moodflix.movie.dto.tmdb.releases.ReleaseDatesDto;
import com.duck.moodflix.movie.dto.tmdb.reviews.ReviewsPageDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * TMDb /movie/{id} 응답을 매핑하는 상위 DTO.
 * - 상위는 Lombok @Getter 로 getXxx() 제공
 * - 하위 DTO는 각 패키지의 record/클래스 조합(예: genres: List<GenreDto(record)>, releaseDates: ReleaseDatesDto(class))
 * - append_to_response: release_dates, images, videos, credits, similar, recommendations, reviews
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TMDbMovieDetailDto {

    // ===== 기본/요약 =====
    @JsonProperty("id")
    private Long id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("original_title")
    private String originalTitle;

    @JsonProperty("status")
    private String status;              // Released/Planned 등

    @JsonProperty("overview")
    private String overview;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("release_date")
    private String releaseDate;         // yyyy-MM-dd

    @JsonProperty("runtime")
    private Integer runtime;            // 분 단위

    // ===== 분류/지표 =====
    @JsonProperty("genres")
    private List<GenreDto> genres;      // record: GenreDto(name)

    @JsonProperty("keywords")
    private KeywordsResponseDto keywords; // record: KeywordsResponseDto(keywords: List<KeywordDto(name)))

    @JsonProperty("vote_average")
    private Double voteAverage;

    @JsonProperty("vote_count")
    private Integer voteCount;

    @JsonProperty("popularity")
    private Double popularity;

    // ===== 생산/통계 =====
    @JsonProperty("budget")
    private Long budget;

    @JsonProperty("revenue")
    private Long revenue;

    @JsonProperty("production_countries")
    private List<CountryDto> productionCountries; // record: CountryDto(iso31661, name)

    @JsonProperty("production_companies")
    private List<CompanyDto> productionCompanies; // record: CompanyDto(id, name)

    // ===== append_to_response 로 받아오는 것들 =====
    @JsonProperty("release_dates")
    private ReleaseDatesDto releaseDates;         // class: getResults() 등

    @JsonProperty("images")
    private ImagesDto images;                     // record/class: posters(), backdrops()

    @JsonProperty("videos")
    private VideosDto videos;                     // record/class: results()

    @JsonProperty("credits")
    private CreditsDto credits;                   // record/class: cast(), crew()

    @JsonProperty("similar")
    private MovieBriefPageDto similar;            // record: results() -> List<MovieBriefDto>

    @JsonProperty("recommendations")
    private MovieBriefPageDto recommendations;    // record: results() -> List<MovieBriefDto>

    @JsonProperty("reviews")
    private ReviewsPageDto reviews;               // record: results() -> List<ReviewDto>
}
