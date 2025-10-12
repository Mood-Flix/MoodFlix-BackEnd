package com.duck.moodflix.auth.config;

import com.duck.moodflix.auth.util.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        log.warn("[AUTH DEBUG] {} {} hasAuthHeader={} len={}",
                request.getMethod(), request.getRequestURI(),
                authHeader != null, authHeader == null ? -1 : authHeader.length());
        String token = resolveToken(request);
        log.warn("[AUTH DEBUG] token.present={}, token.len={}", token != null, token == null ? -1 : token.length());
        // [수정] 토큰 유효성, 기존 인증 여부 확인
        if (token != null
                && jwtTokenProvider.validateToken(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            log.debug("[AUTH] path={}, method={}, hasToken={}, valid={}",
                    request.getRequestURI(), request.getMethod(),
                    token != null, jwtTokenProvider.validateToken(token));

            // [수정] NumberFormatException 예외 처리
            try {
                Claims claims = jwtTokenProvider.getClaimsFromToken(token);
                Long userId = Long.parseLong(claims.getSubject());
                String role = claims.get("role", String.class);

                Collection<? extends GrantedAuthority> authorities = Collections.emptyList();
                if (StringUtils.hasText(role)) {
                    authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
                }

                UserDetails userDetails = new User(userId.toString(), "", authorities);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

                // [추가 개선] 요청 컨텍스트(IP 주소 등)를 인증 객체에 저장
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (NumberFormatException ex) {
                log.warn("Invalid JWT subject (non-numeric). Ignoring token.", ex);
            }
        }

        filterChain.doFilter(request, response);
        var a = SecurityContextHolder.getContext().getAuthentication();
        log.warn("[AUTH DEBUG] afterChain principal={}, authorities={}",
                a == null ? "null" : a.getName(),
                a == null ? "null" : a.getAuthorities());
    }

    /**
     * 요청 헤더(Authorization)에서 'Bearer ' 접두사를 제거하고 토큰 값만 추출합니다.
     * @param request HttpServletRequest
     * @return String | null 추출된 토큰 또는 null
     */
    // JwtAuthenticationFilter.java
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            token = (token != null) ? token.trim() : null;
            log.debug("[AUTH DEBUG] token.present={}, token.len={}",
                    token != null, token != null ? token.length() : 0);
            return (StringUtils.hasText(token)) ? token : null; // ★ 빈 문자열은 null 처리
        }
        return null;
    }
}