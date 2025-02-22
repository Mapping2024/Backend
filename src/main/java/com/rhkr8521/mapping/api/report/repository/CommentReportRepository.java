package com.rhkr8521.mapping.api.report.repository;

import com.rhkr8521.mapping.api.comment.entity.Comment;
import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.report.entity.CommentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    boolean existsByCommentAndMember(Comment comment, Member member);
    void deleteAllByCommentId(Long commentId);
}
