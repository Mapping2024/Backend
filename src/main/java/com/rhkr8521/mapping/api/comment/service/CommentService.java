package com.rhkr8521.mapping.api.comment.service;

import com.rhkr8521.mapping.api.comment.dto.CommentCreateDTO;
import com.rhkr8521.mapping.api.comment.dto.CommentResponseDTO;
import com.rhkr8521.mapping.api.comment.dto.CommentUpdateDTO;
import com.rhkr8521.mapping.api.comment.entity.Comment;
import com.rhkr8521.mapping.api.comment.entity.CommentLike;
import com.rhkr8521.mapping.api.comment.repository.CommentLikeRepository;
import com.rhkr8521.mapping.api.comment.repository.CommentRepository;
import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.member.repository.MemberBlockRepository;
import com.rhkr8521.mapping.api.member.repository.MemberRepository;
import com.rhkr8521.mapping.api.member.service.MemberService;
import com.rhkr8521.mapping.api.memo.entity.Memo;
import com.rhkr8521.mapping.api.memo.repository.MemoRepository;
import com.rhkr8521.mapping.api.watchdog.dto.ProfanityResponseDTO;
import com.rhkr8521.mapping.api.watchdog.service.ProfanityDetectionService;
import com.rhkr8521.mapping.common.exception.NotFoundException;
import com.rhkr8521.mapping.common.exception.UnauthorizedException;
import com.rhkr8521.mapping.common.response.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
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
    private final MemberBlockRepository memberBlockRepository;
    private final ProfanityDetectionService profanityDetectionService;

    // 클라이언트 IP 추출 메소드
    private String extractClientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("WL-Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }
        return clientIp;
    }

    // 댓글 생성
    @Transactional
    public void createComment(CommentCreateDTO commentCreateDTO, Long userId, HttpServletRequest request) {
        // 해당 유저를 찾을 수 없을 경우 예외처리
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));

        // 해당 매모을 찾을 수 없을 경우 예외처리
        Memo memo = memoRepository.findById(commentCreateDTO.getMemoId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));

        // 접속 IP 추출
        String clientIp = extractClientIp(request);

        // 댓글 내용에 대해 비속어 검증
        ProfanityResponseDTO commentResponse = profanityDetectionService.checkTextAndSave(member, commentCreateDTO.getComment());

        Comment comment = Comment.builder()
                .comment(commentResponse.getCensoredText())
                .memo(memo)
                .member(member)
                .rating(commentCreateDTO.getRating())
                .likeCnt(0)
                .createIp(clientIp)
                .modify(false)
                .isDeleted(false)
                .isHidden(false)
                .build();

        commentRepository.save(comment);
    }

    // 댓글 ID 목록 조회 (createdAt 기준 내림차순 정렬)
    @Transactional(readOnly = true)
    public List<Long> getCommentIdsByMemoId(Long memoId) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));

        // createdAt 기준으로 내림차순 정렬된 댓글 목록 조회
        List<Comment> comments = commentRepository.findByMemoOrderByCreatedAtDesc(memo);

        return comments.stream()
                .map(Comment::getId)
                .collect(Collectors.toList());
    }

    // 댓글 상세 조회
    @Transactional(readOnly = true)
    public CommentResponseDTO getCommentDetail(Long commentId, UserDetails userDetails) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.COMMENT_NOTFOUND_EXCEPTION.getMessage()));

        Long userId = null;
        if (userDetails != null) {
            userId = memberService.getUserIdByEmail(userDetails.getUsername());
        }

        // 차단 여부 검사
        if (userId != null) {
            Member currentUser = memberRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));

            if (memberBlockRepository.existsByBlockerAndBlocked(currentUser, comment.getMember())) {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return CommentResponseDTO.builder()
                        .id(comment.getId())
                        .writerId(comment.getMember().getId())
                        .comment("차단된 사용자 입니다.")
                        .rating(comment.getRating())
                        .likeCnt(comment.getLikeCnt())
                        .nickname("(알수없음)")
                        .profileImageUrl(null)
                        .updatedAt(comment.getCreatedAt().format(dateTimeFormatter))
                        .myLike(false)
                        .modify(comment.isModify())
                        .isBlind(true)
                        .build();
            }
        }

        boolean myLike = false;
        if (userId != null) {
            myLike = commentLikeRepository.findByCommentIdAndMemberId(commentId, userId).isPresent();
        }

        return CommentResponseDTO.fromEntity(comment, myLike);
    }

    // 댓글 수정
    @Transactional
    public void updateComment(Long commentId, CommentUpdateDTO commentUpdateDTO, Long userId, HttpServletRequest request) {
        // 해당 유저를 찾을 수 없을 경우 예외처리
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));

        // 댓글이 존재하지 않으면 예외 처리
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.COMMENT_NOTFOUND_EXCEPTION.getMessage()));

        // 해당 댓글의 작성자가 요청한 사용자와 동일한지 검증
        if (!comment.getMember().getId().equals(userId)) {
            throw new UnauthorizedException(ErrorStatus.INVALID_MODIFY_AUTH.getMessage());
        }

        // 접속 IP 추출
        String clientIp = extractClientIp(request);

        // 댓글 내용에 대해 비속어 검증
        ProfanityResponseDTO commentResponse = profanityDetectionService.checkTextAndSave(member, commentUpdateDTO.getComment());

        comment = comment.toBuilder()
                .comment(commentResponse.getCensoredText())
                .rating(commentUpdateDTO.getRating())
                .modify(true)
                .lastModifyIp(clientIp)
                .build();

        commentRepository.save(comment);
    }

    // 댓글 삭제(하드삭제)
//    @Transactional
//    public void deleteComment(Long commentId, Long userId) {
//        // 댓글을 ID로 찾고, 존재하지 않으면 예외 처리
//        Comment comment = commentRepository.findById(commentId)
//                .orElseThrow(() -> new NotFoundException(ErrorStatus.COMMENT_NOTFOUND_EXCPETION.getMessage()));
//
//        // 해당 댓글의 작성자가 요청한 사용자와 동일한지 검증
//        if (!comment.getMember().getId().equals(userId)) {
//            throw new UnauthorizedException(ErrorStatus.INVALID_DELETE_AUTH.getMessage());
//        }
//
//        // CommentLike 삭제
//        commentLikeRepository.deleteAllByCommentId(commentId);
//
//        commentRepository.delete(comment);
//    }

    // 댓글 삭제(소프트삭제)
    @Transactional
    public void deleteComment(Long commentId, Long userId, HttpServletRequest request) {
        // 댓글을 ID로 찾고, 존재하지 않으면 예외 처리
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.COMMENT_NOTFOUND_EXCEPTION.getMessage()));

        // 해당 댓글의 작성자가 요청한 사용자와 동일한지 검증
        if (!comment.getMember().getId().equals(userId)) {
            throw new UnauthorizedException(ErrorStatus.INVALID_DELETE_AUTH.getMessage());
        }

        // 접속 IP 추출
        String clientIp = extractClientIp(request);

        comment = comment.toBuilder()
                .isDeleted(true)
                .lastModifyIp(clientIp)
                .build();

        commentRepository.save(comment);
    }

    // 좋아요 토글
    @Transactional
    public void toggleLike(Long commentId, Long userId) {
        // 댓글과 회원 존재 여부 체크
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.COMMENT_NOTFOUND_EXCEPTION.getMessage()));

        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));

        // 좋아요 상태 확인
        Optional<CommentLike> existingLike = commentLikeRepository.findByCommentIdAndMemberId(commentId, userId);

        if (existingLike.isPresent()) {
            // 좋아요 취소
            commentLikeRepository.delete(existingLike.get());
            commentRepository.decrementLikeCount(commentId);
        } else {
            // 좋아요 추가
            CommentLike articleLike = CommentLike.builder()
                    .comment(comment)
                    .member(member)
                    .build();
            commentLikeRepository.save(articleLike);
            commentRepository.incrementLikeCount(commentId);
        }
    }

}
