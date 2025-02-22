package com.rhkr8521.mapping.api.report.service;

import com.rhkr8521.mapping.api.comment.entity.Comment;
import com.rhkr8521.mapping.api.comment.repository.CommentRepository;
import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.member.repository.MemberRepository;
import com.rhkr8521.mapping.api.member.service.MemberService;
import com.rhkr8521.mapping.api.memo.entity.Memo;
import com.rhkr8521.mapping.api.memo.repository.MemoRepository;
import com.rhkr8521.mapping.api.report.dto.CommentReportRequestDTO;
import com.rhkr8521.mapping.api.report.dto.MemoReportRequestDTO;
import com.rhkr8521.mapping.api.report.entity.CommentReport;
import com.rhkr8521.mapping.api.report.entity.MemoReport;
import com.rhkr8521.mapping.api.report.repository.CommentReportRepository;
import com.rhkr8521.mapping.api.report.repository.MemoReportRepository;
import com.rhkr8521.mapping.common.exception.BadRequestException;
import com.rhkr8521.mapping.common.exception.NotFoundException;
import com.rhkr8521.mapping.common.response.ErrorStatus;
import com.rhkr8521.mapping.slack.SlackNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final MemoRepository memoRepository;
    private final MemberRepository memberRepository;
    private final MemoReportRepository memoReportRepository;
    private final CommentReportRepository commentReportRepository;
    private final CommentRepository commentRepository;
    private final MemberService memberService;
    private final SlackNotificationService slackNotificationService;

    // 메모 신고 기능
    @Transactional
    public void reportMemo(MemoReportRequestDTO reportRequest, UserDetails userDetails) {
        if(reportRequest.getMemoId() == null || reportRequest.getReportReason() == null) {
            throw new BadRequestException(ErrorStatus.VALIDATION_CONTENT_MISSING_EXCEPTION.getMessage());
        }

        // 신고할 메모 조회
        Memo memo = memoRepository.findById(reportRequest.getMemoId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.MEMO_NOTFOUND_EXCEPTION.getMessage()));

        // 신고하는 회원 조회
        Long userId = memberService.getUserIdByEmail(userDetails.getUsername());
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));

        // 이미 신고한 내역이 있는지 체크
        if (memoReportRepository.existsByMemoAndMember(memo, member)) {
            throw new BadRequestException(ErrorStatus.ALREADY_REPORT_MEMO_EXCEPTION.getMessage());
        }

        MemoReport memoReport = MemoReport.builder()
                .memo(memo)
                .member(member)
                .reportReason(reportRequest.getReportReason())
                .build();

        memoReportRepository.save(memoReport);
        slackNotificationService.sendReportMessage("메모", memo.getId(), String.valueOf(reportRequest.getReportReason().getDescription()));
    }

    // 댓글 신고 기능
    @Transactional
    public void reportComment(CommentReportRequestDTO commentReportRequestDTO, UserDetails userDetails) {
        if(commentReportRequestDTO.getCommentId() == null || commentReportRequestDTO.getReportReason() == null) {
            throw new BadRequestException(ErrorStatus.VALIDATION_CONTENT_MISSING_EXCEPTION.getMessage());
        }

        // 신고할 메모 조회
        Comment comment = commentRepository.findById(commentReportRequestDTO.getCommentId())
                .orElseThrow(() -> new NotFoundException(ErrorStatus.COMMENT_NOTFOUND_EXCEPTION.getMessage()));

        // 신고하는 회원 조회
        Long userId = memberService.getUserIdByEmail(userDetails.getUsername());
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorStatus.USER_NOTFOUND_EXCEPTION.getMessage()));

        // 이미 신고한 내역이 있는지 체크
        if (commentReportRepository.existsByCommentAndMember(comment, member)) {
            throw new BadRequestException(ErrorStatus.ALREADY_REPORT_MEMO_EXCEPTION.getMessage());
        }

        CommentReport commentReport = CommentReport.builder()
                .comment(comment)
                .member(member)
                .reportReason(commentReportRequestDTO.getReportReason())
                .build();

        commentReportRepository.save(commentReport);
        slackNotificationService.sendReportMessage("댓글", comment.getId(), String.valueOf(commentReportRequestDTO.getReportReason().getDescription()));
    }
}
