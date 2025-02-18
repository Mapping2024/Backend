package com.rhkr8521.mapping.api.memo.repository;

import com.rhkr8521.mapping.api.memo.entity.Memo;
import com.rhkr8521.mapping.api.memo.entity.MemoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemoLikeRepository extends JpaRepository<MemoLike, Long> {
    Optional<MemoLike> findByMemoIdAndMemberId(Long memoId, Long memberId);
    void deleteAllByMemoId(Long memoId);

    @Query("SELECT m FROM MemoLike ml JOIN ml.memo m WHERE ml.member.id = :userId AND m.isDeleted = false")
    List<Memo> findMemosByMemberId(@Param("userId") Long userId);

}
