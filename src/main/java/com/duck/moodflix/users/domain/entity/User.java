package com.duck.moodflix.users.domain.entity;

import com.duck.moodflix.users.domain.entity.enums.Role;
import com.duck.moodflix.users.domain.entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_email_provider", // 제약 조건 이름
                columnNames = {"email", "provider"} // 유니크해야 할 컬럼 조합
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String email;


    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = true, unique = true) // 카카오 ID는 유일
    private Long kakaoId;

    @Column(length = 20)
    private String gender; // "M", "F"

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 255)
    private String profileImage;

    // "local" | "kakao" | "google" 등 (소셜/로컬 구분)
    @Column(nullable = false, length = 20)
    private String provider;

    @Enumerated(EnumType.STRING) // Enum 타입을 DB에 문자열로 저장
    private UserStatus status = UserStatus.ACTIVE; // 사용자 상태, 기본값은 활성

    @Enumerated(EnumType.STRING) // Enum 이름을 DB에 문자열로 저장 ("USER", "ADMIN")
    @Column(nullable = false)
    private Role role = Role.USER; // [수정] 기본값으로 Role.USER 설정

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    // soft delete: 탈퇴 플래그 (필요시)
    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    // JPA Auditing 등으로 자동화
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.deleted = false;
    }
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public User(String email, String name, String provider) {
        this.email = email;
        this.name = name;
        this.provider = provider;
    }

    /**
     * 프로필 정보를 업데이트합니다.
     */
    public void updateProfile(String name, LocalDate birthDate, String gender, String profileImage) {
        this.name = name;
        this.birthDate = birthDate;
        this.gender = gender;
        this.profileImage = profileImage;
        this.updatedAt = LocalDateTime.now();
    }


    /**
     * 계정을 삭제(비활성화) 상태로 처리하고 개인정보를 비식별화합니다.
     */
    public void deleteAccount() {
        this.status = UserStatus.DELETED;
        // [수정] 유효하고 유일한 이메일로 대체 (예: deleted_1@deleted.local)
        this.email = "deleted_" + this.userId + "@deleted.local";
        this.name = "탈퇴한사용자";
        this.kakaoId = null; // ✅ [추가] 재가입을 위한 kakaoId 해제
        this.deleted = true; // [추가] deleted 플래그 업데이트
    }
}