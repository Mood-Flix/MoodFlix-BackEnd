package com.duck.moodflix.users.repository;

import com.duck.moodflix.users.domain.entity.User;
import com.duck.moodflix.users.domain.entity.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * [추가] 활성(ACTIVE) 상태인 사용자만 이메일로 조회합니다.
     * @param email 조회할 이메일
     * @param status UserStatus.ACTIVE 고정 사용
     * @return Optional<User> 활성 사용자
     */
    Optional<User> findByEmailAndStatus(String email, UserStatus status);

    // [추가] provider와 status까지 고려하여 조회
    Optional<User> findByEmailAndProviderAndStatus(String email, String provider, UserStatus status);

    Optional<User> findByEmailAndProvider(String email, String provider);
    // ✅ [추가] kakaoId로 사용자를 찾는 메서드들
    Optional<User> findByKakaoId(Long kakaoId);
    Optional<User> findByKakaoIdAndStatus(Long kakaoId, UserStatus status);
    @Query("select u.userId from User u where u.name = :username")
    Optional<Long> findIdByUsername(@Param("username") String username);

    @Query("select u.userId from User u where u.email = :email and u.status = :status")
    Optional<Long> findIdByEmailAndStatus(@Param("email") String email,
                                          @Param("status") UserStatus status);
}