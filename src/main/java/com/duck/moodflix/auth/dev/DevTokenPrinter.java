// src/main/java/com/duck/moodflix/auth/dev/DevTokenPrinter.java
package com.duck.moodflix.auth.dev;

import com.duck.moodflix.auth.util.JwtTokenProvider;
import com.duck.moodflix.users.domain.entity.enums.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile({"local","dev"})
@Component
public class DevTokenPrinter implements CommandLineRunner {

    private final JwtTokenProvider jwtTokenProvider;

    public DevTokenPrinter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void run(String... args) {
        String adminToken = jwtTokenProvider.generateToken(1L, Role.ADMIN);
        System.out.println("[DEV] Sample ADMIN token for userId=1:");
        System.out.println(adminToken);
    }
}
