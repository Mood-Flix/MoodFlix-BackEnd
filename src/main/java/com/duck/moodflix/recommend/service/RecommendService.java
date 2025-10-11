package com.duck.moodflix.recommend.service;

import com.duck.moodflix.recommend.client.ModelServerClient;
import com.duck.moodflix.recommend.domain.entity.Recommendation;
import com.duck.moodflix.recommend.domain.entity.UserEmotionInput;
import com.duck.moodflix.recommend.dto.RecommendDtos;
import com.duck.moodflix.recommend.repository.RecommendationRepository;
import com.duck.moodflix.recommend.repository.UserEmotionInputRepository;
import com.duck.moodflix.users.domain.entity.User;
import com.duck.moodflix.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecommendService {

    private final ModelServerClient modelClient;
    private final UserEmotionInputRepository inputRepo;
    private final RecommendationRepository recRepo;
    private final UserRepository userRepo;

    public record Request(String text, Integer topN) {}
    public record Item(Long movieId, String title, List<String> genres, double similarity) {}
    public record Response(String version, List<Item> items, Long logId) {}

    public Mono<Response> byText(Long userId, RecommendDtos.Request req) {
        if (userId == null) throw new IllegalArgumentException("userId is required");

        String text = Optional.ofNullable(req.text()).orElse("");
        int topN = Optional.ofNullable(req.topN()).orElse(20);

        return modelClient.recommendByText(text, topN)
                .publishOn(Schedulers.boundedElastic())                    // 리액티브 + JPA 안전 구역
                .map(res -> saveAll(userId, text, res));
    }

    @Transactional
    protected Response saveAll(Long userId, String text, ModelServerClient.ModelRecommendResponse res) {
        var userRef = userRepo.getReferenceById(userId);               // 존재 보장 가정
        var input = inputRepo.save(UserEmotionInput.builder()
                .user(userRef)                                            //  연관 주입 (FK not null)
                .inputText(text)
                .build());

        var rows = new ArrayList<Recommendation>();
        res.items().forEach(it -> rows.add(
                Recommendation.builder()
                        .user(userRef)                                        //  연관 주입
                        .userEmotionInput(input)                              //  연관 주입
                        .movieId(it.movie_id())
                        .similarityScore(it.similarity())
                        .build()
        ));
        recRepo.saveAll(rows);

        var items = res.items().stream()
                .map(it -> new Item(it.movie_id(), it.title(), it.genres(), it.similarity()))
                .toList();

        return new Response(res.version(), items, input.getId());
    }
}
