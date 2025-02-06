package com.rhkr8521.mapping.api.memo.repository;

import com.rhkr8521.mapping.api.memo.entity.MemoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemoLikeRepository extends JpaRepository<MemoLike, Long> {
    Optional<MemoLike> findByMemoIdAndMemberId(Long memoId, Long memberId);
    void deleteAllByMemoId(Long memoId);
}
