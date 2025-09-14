package com.duck.moodflix.users.controller;

import com.duck.moodflix.users.dto.*;
import com.duck.moodflix.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "User API", description = "사용자 정보 관리 API")
@RestController
@RequestMapping("/api/users") // 기본 경로는 그대로 "/users"
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "프로필 조회", description = "로그인된 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/profile") // 엔드포인트 경로 유지
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        UserProfileResponse profile = userService.getProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "프로필 수정", description = "로그인된 사용자의 프로필 정보를 수정합니다.")
    @PutMapping("/profile") // 엔드포인트 경로 유지
    public ResponseEntity<ProfileEditResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Validated @RequestBody UpdateUserProfileRequest dto) {
        Long userId = Long.parseLong(userDetails.getUsername());
        ProfileEditResponse updatedResult = userService.updateProfile(userId, dto);
        return ResponseEntity.ok(updatedResult);
    }

    @Operation(summary = "회원 탈퇴 (계정 삭제)", description = "로그인된 사용자의 계정을 삭제합니다.")
    @DeleteMapping // 엔드포인트 경로 유지 (DELETE /users)
    public ResponseEntity<Map<String, String>> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Validated @RequestBody DeleteAccountRequest dto) {
        Long userId = Long.parseLong(userDetails.getUsername());
        userService.deleteAccount(userId);
        return ResponseEntity.ok(Map.of("message", "계정 삭제가 완료되었습니다."));
    }
}