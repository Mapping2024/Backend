package com.rhkr8521.mapping.api.comment.service;

import com.rhkr8521.mapping.api.comment.dto.CommentCreateDTO;
import com.rhkr8521.mapping.api.comment.dto.CommentResponseDTO;
import com.rhkr8521.mapping.api.comment.dto.CommentUpdateDTO;
import com.rhkr8521.mapping.api.comment.entity.Comment;
import com.rhkr8521.mapping.api.comment.entity.CommentLike;
import com.rhkr8521.mapping.api.comment.repository.CommentLikeRepository;
import com.rhkr8521.mapping.api.comment.repository.CommentRepository;
import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.member.repository.MemberRepository;
import com.rhkr8521.mapping.api.member.service.MemberService;
import com.rhkr8521.mapping.api.memo.entity.Memo;
import com.rhkr8521.mapping.api.memo.repository.MemoRepository;
import com.rhkr8521.mapping.common.exception.NotFoundException;
import com.rhkr8521.mapping.common.exception.UnauthorizedException;
import com.rhkr8521.mapping.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final MemoRepository memoRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    // 댓글 생성
    @Transactional
    public void createComment(CommentCreateDTO commentCreateDTO, Long userId) {
        // 해당 유저를 찾을 수 없을 경우 예외처리
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));
        // 해당 매모을 찾을 수 없을 경우 예외처리
        Memo memo = memoRepository.findById(commentCreateDTO.getMemoId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));

        Comment comment = Comment.builder()
                .comment(commentCreateDTO.getComment())
                .memo(memo)
                .member(member)
                .rating(commentCreateDTO.getRating())
                .likeCnt(0)
                .modify(false)
                .build();

        commentRepository.save(comment);
    }

    // 댓글 조회
    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getCommentsByMemoId(Long memoId, UserDetails userDetails) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));

        // userId 변수 설정
        Long userId = null;
        if (userDetails != null) {
            userId = memberService.getUserIdByEmail(userDetails.getUsername());
        }

        // 람다에서 참조할 수 있도록 final 변수로 다시 할당
        final Long finalUserId = userId;

        List<Comment> comments = commentRepository.findByMemo(memo);

        return comments.stream()
                .map(c -> {
                    boolean myLike = false;
                    if (finalUserId != null) {
                        myLike = commentLikeRepository.findByCommentIdAndMemberId(c.getId(), finalUserId).isPresent();
                    }
                    return CommentResponseDTO.fromEntity(c, myLike);
                })
                .collect(Collectors.toList());
    }

    // 댓글 수정
    @Transactional
    public void updateComment(Long commentId, CommentUpdateDTO commentUpdateDTO, Long userId) {
        // 댓글이 존재하지 않으면 예외 처리
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.COMMENT_NOTFOUND_EXCPETION.getMessage()));

        // 해당 댓글의 작성자가 요청한 사용자와 동일한지 검증
        if (!comment.getMember().getId().equals(userId)) {
            throw new UnauthorizedException(ErrorStatus.INVALID_MODIFY_AUTH.getMessage());
        }

        comment = comment.toBuilder()
                .comment(commentUpdateDTO.getComment())
                .rating(commentUpdateDTO.getRating())
                .modify(true)
                .build();

        commentRepository.save(comment);
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        // 댓글을 ID로 찾고, 존재하지 않으면 예외 처리
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.COMMENT_NOTFOUND_EXCPETION.getMessage()));

        // 해당 댓글의 작성자가 요청한 사용자와 동일한지 검증
        if (!comment.getMember().getId().equals(userId)) {
            throw new UnauthorizedException(ErrorStatus.INVALID_DELETE_AUTH.getMessage());
        }

        commentRepository.delete(comment);
    }

    // 좋아요 토글
    public void toggleLike(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.COMMENT_NOTFOUND_EXCPETION.getMessage()));

        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));

        // 좋아요 상태 확인
        Optional<CommentLike> existingLike = commentLikeRepository.findByCommentIdAndMemberId(commentId, userId);

        if (existingLike.isPresent()) {
            // 좋아요 취소
            commentLikeRepository.delete(existingLike.get());
            comment = comment.decreaseLikeCnt();
        } else {
            // 좋아요 추가
            CommentLike articleLike = CommentLike.builder()
                    .comment(comment)
                    .member(member)
                    .build();
            commentLikeRepository.save(articleLike);
            comment = comment.increaseLikeCnt();
        }

        commentRepository.save(comment);
    }

}
