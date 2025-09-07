package com.duck.moodflix.auth.service; // 패키지 경로는 실제 환경에 맞게 조정하세요.

import com.duck.moodflix.auth.dto.KakaoUserInfoResponse;
import com.duck.moodflix.auth.dto.LoginResponseDto;
import com.duck.moodflix.auth.util.JwtTokenProvider;
import com.duck.moodflix.users.domain.entity.User;
import com.duck.moodflix.users.domain.entity.enums.Role;
import com.duck.moodflix.users.domain.entity.enums.UserStatus;
import com.duck.moodflix.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;


@Slf4j
@Service
@RequiredArgsConstructor
public class KaKaoService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    // WebClient를 Bean으로 주입받거나 직접 생성하여 사용
    private final WebClient webClient = WebClient.create();

    @Transactional
    public LoginResponseDto oAuthLogin(String accessToken) {
        // 1. 프론트에서 받은 액세스 토큰으로 카카오 서버에 사용자 정보를 요청합니다.
        KakaoUserInfoResponse userInfo = getKakaoUserInfo(accessToken);

        // 2. 받아온 카카오 ID로 우리 DB에 이미 가입된 사용자인지 확인합니다.
        Long kakaoId = userInfo.getId();
        String email = userInfo.getKakaoAccount() != null ? userInfo.getKakaoAccount().getEmail() : null;
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("카카오 이메일 동의가 필요합니다.");
        }
        String provider = "kakao";

        //  [수정] kakaoId -> email 순으로 ACTIVE 상태의 사용자만 조회
        User user = userRepository.findByKakaoIdAndStatus(kakaoId, UserStatus.ACTIVE)
                .or(() -> userRepository.findByEmailAndProviderAndStatus(email, provider, UserStatus.ACTIVE))
                .orElseGet(() -> {
                    String nickname = userInfo.getProperties() != null ? userInfo.getProperties().get("nickname") : null;
                    String safeName = (nickname == null || nickname.isBlank()) ? "카카오사용자" : nickname;

                    return userRepository.save(
                            User.builder()
                                    .email(email)
                                    .name(safeName)
                                    .provider(provider)
                                    .kakaoId(kakaoId)
                                    .role(Role.USER)
                                    .status(UserStatus.ACTIVE)
                                    .build()
                    );
                });

        //  우리 서비스 전용 JWT 생성
        String jwtToken = jwtTokenProvider.generateToken(user.getUserId(), user.getRole());

        //  DTO에 토큰과 사용자 정보를 담아 반환
        return new LoginResponseDto(jwtToken, user.getUserId(), user.getName(), user.getEmail());
    }

    private KakaoUserInfoResponse getKakaoUserInfo(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        return webClient.get()
                .uri(userInfoUrl)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                // 4xx, 5xx 에러 발생 시 예외를 던지도록 설정
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .map(body -> new RuntimeException("카카오 사용자 정보 조회 실패: " + body)))
                .bodyToMono(KakaoUserInfoResponse.class)
                .block(); // 비동기 응답을 동기적으로 기다립니다.
    }
}
