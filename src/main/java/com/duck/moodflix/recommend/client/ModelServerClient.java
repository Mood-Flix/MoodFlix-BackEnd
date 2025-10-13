package com.duck.moodflix.recommend.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ModelServerClient {
    private final WebClient modelClient;

    public Mono<Map<String,Object>> health() {
        return modelClient.get().uri("/health")
                .retrieve().bodyToMono(new ParameterizedTypeReference<>() {});
    }

    public Mono<ModelRecommendResponse> recommendByText(String text, int topN) {
        return modelClient.get()
                .uri(uri -> uri.path("/recommend/by-text")
                        .queryParam("text", text)
                        .queryParam("topN", topN).build())
                .retrieve()
                .bodyToMono(ModelRecommendResponse.class);
    }

    public Mono<Map<String,Object>> runEmbedding(int chunk) {
        return modelClient.post()
                .uri(uri -> uri.path("/admin/embedding/run").queryParam("chunk", chunk).build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {}) // ✅ 정확 제네릭
                .timeout(Duration.ofMinutes(5));
    }

    public record ModelRecommendItem(long movie_id, String title, List<String> genres, double similarity) {}
    public record ModelRecommendResponse(String version, List<ModelRecommendItem> items) {}
}
