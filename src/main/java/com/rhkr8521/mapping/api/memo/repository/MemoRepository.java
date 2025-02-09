package com.rhkr8521.mapping.api.memo.repository;

import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.memo.entity.Memo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemoRepository extends JpaRepository<Memo, Long> {

    @Query(value = "SELECT * FROM memo m " +
            "WHERE (6371 * acos(cos(radians(:lat)) * cos(radians(m.lat)) * " +
            "cos(radians(m.lng) - radians(:lng)) + sin(radians(:lat)) * sin(radians(m.lat)))) <= :km",
            nativeQuery = true)
    List<Memo> findMemosWithinRadius(@Param("lat") double lat,
                                     @Param("lng") double lng,
                                     @Param("km") double km);

    List<Memo> findMemosByMemberId(Long memberId);

}
