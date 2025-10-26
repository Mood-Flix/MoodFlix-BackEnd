package com.duck.moodflix.config;

import com.duck.moodflix.auth.config.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableReactiveMethodSecurity // Reactor 지원 활성화
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .anonymous(anonymous -> anonymous.disable()) // 익명 인증 비활성화
                .securityContext(context -> context
                        .securityContextRepository(new DelegatingSecurityContextRepository(
                                new RequestAttributeSecurityContextRepository(),
                                new HttpSessionSecurityContextRepository()
                        ))
                ) // SecurityContext 유지
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/movies", "/api/movies/{id}", "/api/movies/search").permitAll()
                        .requestMatchers("/", "/index.html", "/error", "/favicon.ico",
                                "/css/**", "/js/**", "/images/**", "/assets/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/recommend/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/recommend/**").authenticated()
                        .requestMatchers("/api/calendar/share/**").permitAll()
                        .requestMatchers("/api/calendar/**").authenticated()
                        .requestMatchers("/api/dev/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            log.warn("Unauthenticated: {} {} (cause={}), SecurityContext={}, Headers={}",
                                    req.getMethod(), req.getRequestURI(),
                                    ex != null ? ex.getMessage() : "null",
                                    SecurityContextHolder.getContext().getAuthentication(),
                                    req.getHeader("Authorization"));
                            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        })
                        .accessDeniedHandler((req, res, ex) -> {
                            var auth = SecurityContextHolder.getContext().getAuthentication();
                            log.error("AccessDenied: {} {}, principal={}, authorities={}",
                                    req.getMethod(), req.getRequestURI(),
                                    auth != null ? auth.getName() : "null",
                                    auth != null ? auth.getAuthorities() : "null");
                            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                        })
                );
        http.addFilterBefore(new LoggingFilter(), JwtAuthenticationFilter.class);
        return http.build();
    }

    private static class LoggingFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            log.debug("FilterChain start: {} {}, SecurityContext={}",
                    request.getMethod(), request.getRequestURI(),
                    SecurityContextHolder.getContext().getAuthentication());
            filterChain.doFilter(request, response);
            log.debug("FilterChain end: {} {}, SecurityContext={}",
                    request.getMethod(), request.getRequestURI(),
                    SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(List.of("*"));
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            return config;
        };
    }
}
