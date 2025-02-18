package com.rhkr8521.mapping.api.member.repository;

import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.member.entity.MemberBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberBlockRepository extends JpaRepository<MemberBlock, Long> {

    boolean existsByBlockerAndBlocked(Member blocker, Member blocked);
    Optional<MemberBlock> findByBlockerAndBlocked(Member blocker, Member blocked);
    List<MemberBlock> findByBlocker(Member blocker);
}
