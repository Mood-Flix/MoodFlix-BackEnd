package com.duck.moodflix.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // API 문서의 기본 정보 설정
        Info info = new Info()
                .title("MoodFlix API Document")
                .version("v1.0.0")
                .description("MoodFlix 프로젝트의 API 명세서입니다.");

        // JWT 인증 스키마 설정
        String jwtSchemeName = "Bearer Authentication";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        List<Server> servers = List.of(
                new Server().url("http://localhost:8080"),
                new Server().url("https://api.moodflix.store")
        );

        return new OpenAPI()
                .servers(servers)
                .info(info)
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}