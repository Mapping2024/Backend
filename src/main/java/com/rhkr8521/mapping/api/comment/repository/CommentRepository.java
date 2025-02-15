package com.rhkr8521.mapping.api.comment.repository;

import com.rhkr8521.mapping.api.comment.entity.Comment;
import com.rhkr8521.mapping.api.memo.entity.Memo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByMemoOrderByCreatedAtDesc(Memo memo);

    List<Comment> findByMemoId(Long memoId);  // 특정 메모의 댓글 찾기
    void deleteAllByMemoId(Long memoId);

    @Query("SELECT DISTINCT c.memo FROM Comment c WHERE c.member.id = :userId")
    List<Memo> findDistinctMemoByMemberId(@Param("userId") Long userId);
}
