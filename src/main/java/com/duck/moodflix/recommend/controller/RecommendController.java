package com.duck.moodflix.recommend.controller;

import com.duck.moodflix.recommend.client.ModelServerClient;
import com.duck.moodflix.recommend.dto.RecommendDtos;
import com.duck.moodflix.recommend.service.RecommendService;
import com.duck.moodflix.users.domain.entity.enums.UserStatus;
import com.duck.moodflix.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
@Tag(name = "Recommend", description = "영화 추천 API (백엔드 → 모델서버 프록시)")
public class RecommendController {

    private final RecommendService service;
    private final ModelServerClient client;
    private final UserRepository userRepository;

    @PostMapping("/by-text")
    public Mono<RecommendDtos.Response> byText(
            @RequestBody RecommendDtos.Request req,
            @AuthenticationPrincipal User principal) {
        log.info("[CTRL DEBUG] /by-text principal={}", principal);
        log.debug("SecurityContext before Mono: {}", SecurityContextHolder.getContext().getAuthentication());
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }

        String username = principal.getUsername();
        Long userId;

        if (isNumeric(username)) {
            userId = Long.parseLong(username);
            if (!userRepository.existsByUserIdAndStatus(userId, UserStatus.ACTIVE)) {
                log.warn("Inactive or non-existent user: userId={}", userId);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not active");
            }
        } else {
            userId = userRepository.findIdByEmailAndStatus(username, UserStatus.ACTIVE)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Mono.deferContextual(context -> {
                    return ReactiveSecurityContextHolder.getContext()
                            .map(securityContext -> {
                                log.debug("ReactorContext SecurityContext: {}", securityContext.getAuthentication());
                                return securityContext;
                            })
                            .then(service.byText(userId, req));
                })
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .doOnSuccess(response -> log.debug("Service response: {}", response))
                .doOnError(error -> log.error("Service error: {}", error.getMessage(), error))
                .doFinally(signal -> {
                    log.debug("Mono completed, SecurityContext={}", SecurityContextHolder.getContext().getAuthentication());
                    ReactiveSecurityContextHolder.getContext()
                            .doOnNext(ctx -> log.debug("Reactive SecurityContext after Mono: {}", ctx.getAuthentication()))
                            .doOnError(err -> log.error("Error accessing Reactive SecurityContext: {}", err.getMessage()))
                            .subscribe();
                });
    }

    private boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false;
        return true;
    }

    @Operation(
            summary = "임베딩 배치 실행(관리자)",
            description = "모델 서버에서 MySQL 영화를 읽어 청크 단위로 감정 임베딩을 수행합니다.",
            parameters = {
                    @Parameter(name = "chunk", description = "한 번에 처리할 레코드 수", example = "200")
            },
            responses = @ApiResponse(responseCode = "200", description = "실행 결과(JSON)")
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/embedding/run")
    public Mono<Map<String, Object>> runEmbedding(@RequestParam(defaultValue = "200") int chunk) {
        return client.runEmbedding(chunk);
    }
}