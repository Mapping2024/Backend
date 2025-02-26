package com.rhkr8521.mapping.api.report.repository;

import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.memo.entity.Memo;
import com.rhkr8521.mapping.api.report.entity.MemoReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemoReportRepository extends JpaRepository<MemoReport, Long> {

    boolean existsByMemoAndMember(Memo memo, Member member);
    void deleteAllByMemoId(Long memoId);
}
