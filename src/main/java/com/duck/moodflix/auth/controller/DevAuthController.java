//package com.duck.moodflix.auth.controller;
//
//import com.duck.moodflix.auth.util.JwtTokenProvider;
//import com.duck.moodflix.users.domain.entity.enums.Role;
//import org.springframework.context.annotation.Profile;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@Profile({"local","dev"}) // 운영에서 자동 비활성화
//@RestController
//@RequestMapping("/api/dev/auth")
//public class DevAuthController {
//
//    private final JwtTokenProvider jwt;
//
//    public DevAuthController(JwtTokenProvider jwt) {
//        this.jwt = jwt;
//    }
//
//    // 예: GET /api/dev/auth/token?userId=1&role=ADMIN
//    @GetMapping("/token")
//    public ResponseEntity<Map<String, Object>> issueToken(
//            @RequestParam Long userId,
//            @RequestParam Role role
//    ) {
//        String token = jwt.generateToken(userId, role);
//        return ResponseEntity.ok(Map.of(
//                "token", token,
//                "userId", userId,
//                "role", role.name()
//        ));
//    }
//}
