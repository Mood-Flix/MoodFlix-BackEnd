package com.duck.moodflix.config;

import com.duck.moodflix.auth.config.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity // 메서드 레벨의 보안 설정을 활성화합니다.
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/movies", "/api/movies/{id}", "/api/movies/search").permitAll()
                        .requestMatchers("/", "/index.html", "/error", "/favicon.ico",
                                "/css/**", "/js/**", "/images/**", "/assets/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 보호 구간
                        .requestMatchers("/api/recommend/admin/**").hasRole("ADMIN")                 // ← 기존 permitAll 제거 (안전)
                        .requestMatchers("/api/users/**").hasAnyRole("USER","ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/recommend/**").permitAll()
                        .requestMatchers("/api/dev/auth/**").permitAll()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> { //  토큰 없음/무효 → 401
                            log.warn("Unauthenticated: {} {} (token missing/invalid)", req.getMethod(), req.getRequestURI());
                            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                        .accessDeniedHandler((req, res, ex) -> {      //  권한 부족 → 403
                            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                            log.error("AccessDenied: {} {}, principal={}, authorities={}",
                                    req.getMethod(), req.getRequestURI(),
                                    auth != null ? auth.getName() : "null",
                                    auth != null ? auth.getAuthorities() : "null");
                            res.sendError(HttpServletResponse.SC_FORBIDDEN);
                        })
        );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 🔽 허용할 출처에 실제 프론트엔드 도메인을 추가합니다.
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "https://www.moodflix.store", "https://api.moodflix.store"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
