package com.duck.moodflix.auth.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean("tmdbWebClient")
    public WebClient tmdbWebClient(TmdbProperties tmdbProperties) {
        HttpClient httpClient = HttpClient.create()
                .compress(true)
                .responseTimeout(Duration.ofSeconds(10))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000);

        // ✅ [핵심 수정] 메모리 버퍼 크기를 늘리는 ExchangeStrategies 생성
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB로 설정
                .build();

        WebClient.Builder b = WebClient.builder()
                .baseUrl("https://api.themoviedb.org/3")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip")
                .exchangeStrategies(exchangeStrategies); // 생성한 전략을 WebClient에 적용

        // v4 토큰이 있으면 Authorization 헤더 적용
        if (tmdbProperties.getBearerToken() != null && !tmdbProperties.getBearerToken().isBlank()) {
            b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tmdbProperties.getBearerToken());
        }

        return b.build();
    }

    @Bean("kakaoWebClient")
    public WebClient kakaoWebClient() {
        HttpClient httpClient = HttpClient.create()
                .compress(true)
                .responseTimeout(Duration.ofSeconds(10))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000);

        return WebClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip")
                .build();
    }


}