package com.rhkr8521.mapping.api.member.repository;

import com.rhkr8521.mapping.api.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    Optional<Member> findByRefreshToken(String refreshToken);

    Optional<Member> findBySocialId(String socialId);

    List<Member> findAllByDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);

    boolean existsByNickname(String nickname);
}
