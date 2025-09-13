package com.duck.moodflix.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tmdb")
@Getter
@Setter
public class TmdbProperties {
    private String apiKey;

    /** v4 Read Access Token (Bearer) — 있으면 Authorization 헤더에 사용 */
    private String bearerToken;

    private final String posterBaseUrl = "https://image.tmdb.org/t/p/w500";
}
