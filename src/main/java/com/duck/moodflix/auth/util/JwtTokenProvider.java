package com.duck.moodflix.auth.util;

import com.duck.moodflix.users.domain.entity.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMilliseconds;

    public JwtTokenProvider(@Value("${jwt.secret.key}") String secretKey,
                            @Value("${jwt.expiration.ms}") long expirationMilliseconds) {
        // [수정] JWT 비밀키의 길이가 최소 32바이트인지 검증
        if (secretKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT 비밀키는 최소 32바이트 이상이어야 합니다.");
        }

        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.expirationMilliseconds = expirationMilliseconds;
    }

    // [수정] 파라미터를 Set<Role>에서 단일 Role로 변경
    public String generateToken(Long userId, Role role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMilliseconds);

        return Jwts.builder()
                .subject(Long.toString(userId)) // subject 클레임 설정
                .claim("role", role.name())     // "role" 커스텀 클레임 추가
                .issuedAt(now)                  // 발급 시간 설정
                .expiration(expiryDate)           // 만료 시간 설정
                .signWith(key, Jwts.SIG.HS384)   // 서명
                .compact();                     // JWT 문자열 생성
    }


    public Claims getClaimsFromToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    // JwtTokenProvider.java
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)                  // 같은 key 필드 사용
                    .clockSkewSeconds(60)             // 서버/클라 시계 오차 허용
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("[JWT] expired at {}", e.getClaims().getExpiration());
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("[JWT] signature invalid (secret/alg mismatch): {}", e.getMessage());
        } catch (io.jsonwebtoken.JwtException e) {  // MalformedJwtException 포함
            log.warn("[JWT] invalid jwt: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("[JWT] unknown error: {}", e.toString());
        }
        return false;
    }

}
