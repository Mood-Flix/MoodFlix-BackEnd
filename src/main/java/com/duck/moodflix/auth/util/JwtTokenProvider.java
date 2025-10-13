package com.duck.moodflix.auth.util;

import com.duck.moodflix.users.domain.entity.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMilliseconds;
    private final String keyFingerprint; // SHA-256 of normalized secret
    private final String algName = "HS384"; // 발급 시 사용할 알고리즘 명시

    public JwtTokenProvider(@Value("${jwt.secret.key}") String secretKey,
                            @Value("${jwt.expiration.ms}") long expirationMilliseconds) {

        // 1) 공백/개행 제거 (중요)
        String normalized = secretKey == null ? "" : secretKey.strip();
        byte[] keyBytes = normalized.getBytes(StandardCharsets.UTF_8);

        // 2) HS384는 권장 48바이트 이상
        if (keyBytes.length < 48) {
            throw new IllegalArgumentException("JWT 비밀키는 HS384 사용시 최소 48바이트 이상을 권장합니다. 현재=" + keyBytes.length + " bytes");
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMilliseconds = expirationMilliseconds;
        this.keyFingerprint = sha256Base64(keyBytes);

    }

    /** 토큰 발급 */
    public String generateToken(Long userId, Role role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMilliseconds);

        String token = Jwts.builder()
                .subject(Long.toString(userId))
                .claim("role", Objects.requireNonNull(role, "role must not be null").name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS384) // ← 발급 alg 고정
                .compact();

        return token;
    }

    /** 토큰에서 Claims 추출 (검증 포함) */
    public Claims getClaimsFromToken(String token) {
        if (!org.springframework.util.StringUtils.hasText(token)) {
            log.warn("[JWT] getClaimsFromToken: empty token");
            return null;
        }

        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .clockSkewSeconds(120)
                .build()
                .parseSignedClaims(token);

        Header header = jws.getHeader();
        Claims payload = jws.getPayload();
        return payload;
    }

    /** 토큰 유효성 검증 */
    public boolean validateToken(String token) {
        try {
            if (!org.springframework.util.StringUtils.hasText(token)) {
                log.warn("[JWT] validate: empty token string");
                return false;
            }

            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .clockSkewSeconds(120)
                    .build()
                    .parseSignedClaims(token);

            Header header = jws.getHeader();
            Claims c = jws.getPayload();

            // HS384 알고리즘 강제 검사
            if (!algName.equals(header.getAlgorithm())) {
                log.warn("[JWT] invalid algorithm: expected=HS384, actual={}", header.getAlgorithm());
                return false;
            }

            if (log.isDebugEnabled()) {
                log.debug("[JWT] validate ok: alg={}, exp={}, key.fp={}",
                        header.getAlgorithm(), c.getExpiration(), keyFingerprint);
            }
            return true;

        } catch (ExpiredJwtException e) {
            log.warn("[JWT] expired: exp={}, now={}, sub={}",
                    safeDate(e.getClaims().getExpiration()), new Date(), e.getClaims().getSubject());
        } catch (SignatureException e) {
            log.warn("[JWT] signature invalid (secret/alg mismatch?): msg={}, key.fp={}",
                    e.getMessage(), keyFingerprint);
        } catch (MalformedJwtException e) {
            log.warn("[JWT] malformed jwt: {}, key.fp={}", e.getMessage(), keyFingerprint);
        } catch (io.jsonwebtoken.JwtException e) {
            log.warn("[JWT] invalid jwt: {}, key.fp={}", e.getMessage(), keyFingerprint);
        } catch (Exception e) {
            log.warn("[JWT] unknown error: {}, key.fp={}", e.toString(), keyFingerprint);
        }
        return false;
    }

    // ======= helpers =======
    private static String sha256Base64(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static Date safeDate(Date d) {
        return d == null ? null : new Date(d.getTime());
    }
}