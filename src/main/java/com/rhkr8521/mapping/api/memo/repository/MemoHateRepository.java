package com.rhkr8521.mapping.api.memo.repository;

import com.rhkr8521.mapping.api.memo.entity.MemoHate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemoHateRepository extends JpaRepository<MemoHate, Long> {
    Optional<MemoHate> findByMemoIdAndMemberId(Long memoId, Long memberId);
}
