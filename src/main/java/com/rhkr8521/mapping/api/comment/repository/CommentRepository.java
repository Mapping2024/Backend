package com.rhkr8521.mapping.api.comment.repository;

import com.rhkr8521.mapping.api.comment.entity.Comment;
import com.rhkr8521.mapping.api.memo.entity.Memo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByMemoOrderByCreatedAtDesc(Memo memo);

    List<Comment> findByMemoId(Long memoId);  // 특정 메모의 댓글 찾기
    void deleteAllByMemoId(Long memoId);
}
