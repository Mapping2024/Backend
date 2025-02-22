package com.rhkr8521.mapping.api.report.controller;

import com.rhkr8521.mapping.api.report.dto.CommentReportRequestDTO;
import com.rhkr8521.mapping.api.report.dto.MemoReportRequestDTO;
import com.rhkr8521.mapping.api.report.service.ReportService;
import com.rhkr8521.mapping.common.exception.BadRequestException;
import com.rhkr8521.mapping.common.response.ApiResponse;
import com.rhkr8521.mapping.common.response.ErrorStatus;
import com.rhkr8521.mapping.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Report", description = "Report 관련 API 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/report")
public class ReportController {

    private final ReportService reportService;

    @Operation(
            summary = "메모 신고 API",
            description = "해당 메모를 신고합니다. <br>" +
                    "<p>" +
                    "신고 사유 리스트 :<br>" +
                    "SPAM: 스팸홍보/도배글입니다.<br>" +
                    "OBSCENE: 음란물입니다.<br>" +
                    "ILLEGAL_INFORMATION: 불법정보를 포함하고 있습니다.<br>" +
                    "HARMFUL_TO_MINORS: 청소년에게 유해한 내용입니다.<br>" +
                    "OFFENSIVE_EXPRESSION: 욕설/생명경시/혐오/차벌적 표현입니다.<br>" +
                    "PRIVACY_EXPOSURE: 개인정보 노출 게시물입니다.<br>" +
                    "UNPLEASANT_EXPRESSION: 불쾌한 표현이 있습니다.<br>" +
                    "OTHER: 기타"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메모 신고 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "해당 메모는 이미 신고처리 되었습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 메모를 찾을 수 없습니다.")
    })
    @PostMapping("/memo/report")
    public ResponseEntity<ApiResponse<Void>> reportMemo(
            @RequestBody MemoReportRequestDTO memoReportRequestDTO,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (memoReportRequestDTO.getMemoId() == null || memoReportRequestDTO.getReportReason() == null) {
            throw new BadRequestException(ErrorStatus.VALIDATION_CONTENT_MISSING_EXCEPTION.getMessage());
        }

        reportService.reportMemo(memoReportRequestDTO, userDetails);
        return ApiResponse.success_only(SuccessStatus.REPORT_MEMO_SUCCESS);
    }

    @Operation(
            summary = "댓글 신고 API",
            description = "해당 댓글을 신고합니다. <br>" +
                    "<p>" +
                    "신고 사유 리스트 :<br>" +
                    "SPAM: 스팸홍보/도배글입니다.<br>" +
                    "OBSCENE: 음란물입니다.<br>" +
                    "ILLEGAL_INFORMATION: 불법정보를 포함하고 있습니다.<br>" +
                    "HARMFUL_TO_MINORS: 청소년에게 유해한 내용입니다.<br>" +
                    "OFFENSIVE_EXPRESSION: 욕설/생명경시/혐오/차벌적 표현입니다.<br>" +
                    "PRIVACY_EXPOSURE: 개인정보 노출 게시물입니다.<br>" +
                    "UNPLEASANT_EXPRESSION: 불쾌한 표현이 있습니다.<br>" +
                    "OTHER: 기타"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 신고 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "해당 댓글는 이미 신고처리 되었습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 댓글를 찾을 수 없습니다.")
    })
    @PostMapping("/comment/report")
    public ResponseEntity<ApiResponse<Void>> reportComment(
            @RequestBody CommentReportRequestDTO commentReportRequestDTO,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (commentReportRequestDTO.getCommentId() == null || commentReportRequestDTO.getReportReason() == null) {
            throw new BadRequestException(ErrorStatus.VALIDATION_CONTENT_MISSING_EXCEPTION.getMessage());
        }

        reportService.reportComment(commentReportRequestDTO, userDetails);
        return ApiResponse.success_only(SuccessStatus.REPORT_COMMENT_SUCCESS);
    }
}
