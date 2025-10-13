package com.duck.moodflix.auth.controller;

import com.duck.moodflix.auth.util.JwtTokenProvider;
import com.duck.moodflix.users.domain.entity.enums.Role;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Profile({"local","dev"})
@RestController
@RequestMapping("/api/dev/auth")
public class DevAuthDebugController {

    private final JwtTokenProvider jwt;

    public DevAuthDebugController(JwtTokenProvider jwt) {
        this.jwt = jwt;
    }

    /** 토큰 발급: GET /api/dev/auth/token?userId=1&role=ADMIN */
    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> issueToken(
            @RequestParam Long userId,
            @RequestParam Role role
    ) {
        String token = jwt.generateToken(userId, role);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("userId", userId);
        body.put("role", role.name());
        return ResponseEntity.ok(body);
    }

    /** 현재 헤더 토큰 유효성 확인 (전역 Authorize 사용 권장) */
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/whoami")
    public ResponseEntity<Map<String, Object>> whoami(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Map<String, Object> res = new LinkedHashMap<>();

        // 헤더 없음
        if (authHeader == null || authHeader.isBlank()) {
            res.put("valid", false);
            res.put("reason", "missing Authorization header");
            return ResponseEntity.ok(res);
        }

        // Bearer 접두사 체크
        if (!authHeader.startsWith("Bearer ")) {
            res.put("valid", false);
            res.put("reason", "Authorization header must start with 'Bearer '");
            return ResponseEntity.ok(res);
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            res.put("valid", false);
            res.put("reason", "empty token");
            return ResponseEntity.ok(res);
        }

        boolean valid = jwt.validateToken(token);
        if (!valid) {
            res.put("valid", false);
            res.put("reason", "invalid/expired/signature");
            return ResponseEntity.ok(res);
        }

        Claims c = jwt.getClaimsFromToken(token);
        res.put("valid", true);
        res.put("sub", c.getSubject());
        res.put("role", c.get("role"));
        res.put("iat", c.getIssuedAt());
        res.put("exp", c.getExpiration());

        return ResponseEntity.ok(res);
    }
}
