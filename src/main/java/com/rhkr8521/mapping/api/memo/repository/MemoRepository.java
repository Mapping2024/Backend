package com.rhkr8521.mapping.api.memo.repository;

import com.rhkr8521.mapping.api.memo.entity.Memo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@Repository
public interface MemoRepository extends JpaRepository<Memo, Long> {

    @Query(value = "SELECT * FROM memo m " +
            "WHERE m.is_deleted = false " +
            "AND (6371 * acos(cos(radians(:lat)) * cos(radians(m.lat)) * " +
            "cos(radians(m.lng) - radians(:lng)) + sin(radians(:lat)) * sin(radians(m.lat)))) <= :km",
            nativeQuery = true)
    List<Memo> findMemosWithinRadius(@Param("lat") double lat,
                                     @Param("lng") double lng,
                                     @Param("km") double km);

    List<Memo> findByMemberIdAndIsDeletedFalse(Long memberId);

    // 좋아요 증가
    @Modifying
    @Query("update Memo m set m.likeCnt = m.likeCnt + 1 where m.id = :memoId")
    void incrementLikeCount(@Param("memoId") Long memoId);

    // 좋아요 감소
    @Modifying
    @Query("update Memo m set m.likeCnt = case when m.likeCnt > 0 then m.likeCnt - 1 else 0 end where m.id = :memoId")
    void decrementLikeCount(@Param("memoId") Long memoId);

    // 싫어요 증가
    @Modifying
    @Query("update Memo m set m.hateCnt = m.hateCnt + 1 where m.id = :memoId")
    void incrementHateCount(@Param("memoId") Long memoId);

    // 싫어요 감소
    @Modifying
    @Query("update Memo m set m.hateCnt = case when m.hateCnt > 0 then m.hateCnt - 1 else 0 end where m.id = :memoId")
    void decrementHateCount(@Param("memoId") Long memoId);

}
