package com.rhkr8521.mapping.scheduler;

import com.rhkr8521.mapping.api.aws.s3.S3Service;
import com.rhkr8521.mapping.api.comment.entity.Comment;
import com.rhkr8521.mapping.api.comment.repository.CommentLikeRepository;
import com.rhkr8521.mapping.api.comment.repository.CommentRepository;
import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.member.repository.MemberBlockRepository;
import com.rhkr8521.mapping.api.member.repository.MemberRepository;
import com.rhkr8521.mapping.api.memo.entity.Memo;
import com.rhkr8521.mapping.api.memo.repository.MemoHateRepository;
import com.rhkr8521.mapping.api.memo.repository.MemoLikeRepository;
import com.rhkr8521.mapping.api.report.repository.CommentReportRepository;
import com.rhkr8521.mapping.api.report.repository.MemoReportRepository;
import com.rhkr8521.mapping.api.memo.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberDataCleanupScheduler {

    private final MemberRepository memberRepository;
    private final MemoRepository memoRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final MemoLikeRepository memoLikeRepository;
    private final MemoHateRepository memoHateRepository;
    private final MemoReportRepository memoReportRepository;
    private final S3Service s3Service;
    private final MemberBlockRepository memberBlockRepository;
    private final CommentReportRepository commentReportRepository;

    // 매일 자정에 실행 (cron 표현식: "0 0 0 * * *")
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupDeletedMembers() {
        // 90일 이후 모든 데이터 삭제
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        // deleted가 true이고 deletedAt이 cutoff 이전인 회원 목록 조회
        List<Member> membersToDelete = memberRepository.findAllByDeletedTrueAndDeletedAtBefore(cutoff);

        for (Member member : membersToDelete) {
            Long memberId = member.getId();

            // 1. 해당 회원이 작성한 모든 메모 조회
            List<Memo> memos = memoRepository.findByMemberId(memberId);
            for (Memo memo : memos) {
                Long memoId = memo.getId();

                // 1-1. 메모에 포함된 이미지가 있으면 S3에서 실제 파일 삭제
                if (memo.getImages() != null) {
                    memo.getImages().forEach(image -> s3Service.deleteFile(image.getImageUrl()));
                }

                // 1-2. 메모에 연관된 좋아요, 싫어요, 신고 삭제
                memoLikeRepository.deleteAllByMemoId(memoId);
                memoHateRepository.deleteAllByMemoId(memoId);
                memoReportRepository.deleteAllByMemoId(memoId);

                // 1-3. 해당 메모의 댓글(및 댓글 좋아요) 삭제
                List<Comment> commentsForMemo = commentRepository.findByMemoId(memoId);
                for (Comment comment : commentsForMemo) {
                    // 댓글 좋아요 삭제
                    commentLikeRepository.deleteAllByCommentId(comment.getId());
                    // 댓글 신고 삭제
                    commentReportRepository.deleteAllByCommentId(comment.getId());
                    // 댓글 삭제
                    commentRepository.delete(comment);
                }

                // 1-4. 메모 자체 삭제
                memoRepository.delete(memo);
            }

            // 2. 해당 회원이 작성한 댓글 조회 및 삭제
            List<Comment> comments = commentRepository.findByMemberId(memberId);
            for (Comment comment : comments) {
                // 댓글 좋아요 삭제
                commentLikeRepository.deleteAllByCommentId(comment.getId());
                // 댓글 신고 삭제
                commentReportRepository.deleteAllByCommentId(comment.getId());
                // 댓글 삭제
                commentRepository.delete(comment);
            }

            // 3. 해당 회원과 관련된 블록 정보(MemberBlock) 삭제
            memberBlockRepository.deleteAllByBlockerIdOrBlockedId(memberId, memberId);

            // 4. 회원의 프로필 이미지가 존재하면 S3에서 삭제
            if (member.getImageUrl() != null) {
                s3Service.deleteFile(member.getImageUrl());
            }

            // 4. 최종적으로 회원 자체 삭제
            memberRepository.delete(member);
        }
    }
}