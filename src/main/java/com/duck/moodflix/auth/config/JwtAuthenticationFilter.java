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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        log.debug("JwtAuthenticationFilter start: {} {}", request.getMethod(), request.getRequestURI());
        String token = resolveToken(request);
        log.debug("Resolved token: {}", token != null ? token : "null");

        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                Claims claims = jwtTokenProvider.getClaimsFromToken(token);
                log.debug("Claims: sub={}, role={}", claims.getSubject(), claims.get("role"));
                Long userId = Long.parseLong(claims.getSubject());
                String role = claims.get("role", String.class);

                Collection<? extends GrantedAuthority> authorities = Collections.emptyList();
                if (StringUtils.hasText(role)) {
                    authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
                }

                UserDetails userDetails = new User(userId.toString(), "", authorities);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authentication set: {}", SecurityContextHolder.getContext().getAuthentication());
            } catch (NumberFormatException ex) {
                log.warn("Invalid JWT subject (non-numeric): {}", ex.getMessage());
            } catch (Exception ex) {
                log.warn("Failed to process token: {}", ex.getMessage(), ex);
            }
        } else {
            log.debug("Invalid or missing token: {}", token);
        }

        filterChain.doFilter(request, response);
        log.debug("JwtAuthenticationFilter end: SecurityContext={}", SecurityContextHolder.getContext().getAuthentication());
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        log.debug("Authorization header: {}", bearerToken);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7).trim();
            log.debug("Extracted token: {}", token);
            return StringUtils.hasText(token) ? token : null;
        }
        return null;
    }
}