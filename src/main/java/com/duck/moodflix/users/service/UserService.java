package com.duck.moodflix.users.service;

import com.duck.moodflix.users.domain.entity.User;
import com.duck.moodflix.users.domain.entity.enums.UserStatus;
import com.duck.moodflix.users.dto.ProfileEditResponse;
import com.duck.moodflix.users.dto.UpdateUserProfileRequest;
import com.duck.moodflix.users.dto.UserProfileResponse;
import com.duck.moodflix.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public UserProfileResponse getProfile(Long userId) {
        User user = findUserById(userId);
        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .birthDate(user.getBirthDate() != null ? user.getBirthDate().toString() : null)
                .gender(user.getGender())
                .profileImage(user.getProfileImage())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Transactional
    public ProfileEditResponse updateProfile(Long userId, UpdateUserProfileRequest dto) {
        User user = findUserById(userId);
        LocalDate birthDate = (dto.getBirthDate() != null && !dto.getBirthDate().isBlank()) ? LocalDate.parse(dto.getBirthDate()) : null;

        user.updateProfile(dto.getName(), birthDate, dto.getGender(), dto.getProfileImage());

        return ProfileEditResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .profileImage(user.getProfileImage())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Transactional
    // [수정] 비밀번호 파라미터 제거
    public void deleteAccount(Long userId) {
        User user = findUserById(userId);

        if (user.getStatus() == UserStatus.DELETED) {
            return;
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }

}