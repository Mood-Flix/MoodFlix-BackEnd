package com.duck.moodflix.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class KakaoUserInfoResponse {
    private Long id;
    private Map<String, String> properties;

    @JsonProperty("kakao_account") // JSON의 snake_case와 Java의 camelCase를 매핑
    private KakaoAccount kakaoAccount;

    @Data
    public static class KakaoAccount {
        private String email;
        // 필요한 다른 정보들 (프로필 등)이 있다면 추가
    }
}


