package com.duck.moodflix.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter // ✅ [추가] Jackson이 JSON 데이터를 객체로 변환(역직렬화)할 때 필요합니다.
@NoArgsConstructor
public class KakaoLoginRequest {

    // ✅ [추가] 토큰 값이 비어있으면 유효성 검사에서 실패하도록 설정합니다.
    @NotBlank(message = "Access token cannot be blank")
    // ✅ [추가] 프론트엔드가 'accessToken' 또는 'access_token' 어떤 키로 보내도 받을 수 있습니다.
    @JsonAlias({"accessToken", "access_token"})
    private String accessToken;
}

