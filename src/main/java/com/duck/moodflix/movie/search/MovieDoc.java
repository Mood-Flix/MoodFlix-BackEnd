package com.duck.moodflix.movie.search;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "movies")
@Setting(settingPath = "elasticsearch/analyzer-settings.json") // 설정 파일 경로
public class MovieDoc {
    @Id
    private Long id;

    @Field(type = FieldType.Long)
    private Long tmdbId;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "korean_analyzer"),
            otherFields = {
                    // 정확 일치(term) 조회용 서브필드
                    @InnerField(suffix = "exact", type = FieldType.Keyword, normalizer = "lowercase_normalizer")
            }
    )
    private String title;

    @MultiField(
            mainField = @Field(
                    type = FieldType.Text,
                    analyzer = "chosung_edge_analyzer",
                    searchAnalyzer = "keyword_lower"         // ← 여기!
            ),
            otherFields = {
                    @InnerField(
                            suffix = "ngram",
                            type = FieldType.Text,
                            analyzer = "chosung_ngram_analyzer",
                            searchAnalyzer = "keyword_lower"     // ← 여기!
                    ),
                    @InnerField(
                            suffix = "raw",
                            type = FieldType.Keyword
                    )
            }
    )
    private String titleChoseong;

    @Field(type = FieldType.Keyword) // URL은 보통 keyword 타입이 적합합니다.
    private String posterUrl;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private String genre;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private List<String> keywords;

    @Field(type = FieldType.Boolean)
    private boolean adult;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate releaseDate;

    @Field(type = FieldType.Double)
    private Double popularity;

    @Field(type = FieldType.Double)
    private Double voteAverage;
}