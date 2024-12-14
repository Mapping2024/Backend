package com.rhkr8521.mapping.api.comment.controller;

import com.rhkr8521.mapping.api.comment.dto.CommentCreateDTO;
import com.rhkr8521.mapping.api.comment.dto.CommentResponseDTO;
import com.rhkr8521.mapping.api.comment.dto.CommentUpdateDTO;
import com.rhkr8521.mapping.api.comment.service.CommentService;
import com.rhkr8521.mapping.api.member.service.MemberService;
import com.rhkr8521.mapping.common.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import com.rhkr8521.mapping.common.response.ApiResponse;
import com.rhkr8521.mapping.common.response.ErrorStatus;
import com.rhkr8521.mapping.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Comment", description = "댓글 관련 API 입니다.")
@RestController
@RequestMapping("/api/v2/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final MemberService memberService;

    @Operation(
            summary = "댓글 작성 API",
            description = "메모에 댓글을 작성합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "댓글 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "댓글이 입력되지 않았습니다. / 메모 ID가 입력되지 않았습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "메모를 찾을 수 없습니다.")
    })
    @PostMapping("/new")
    public ResponseEntity<ApiResponse<Void>> createComment(
            @AuthenticationPrincipal UserDetails userDetails,
            CommentCreateDTO commentCreateDTO
    ) {

        // Comment 누락시 예외처리
        if (commentCreateDTO.getComment() == null || commentCreateDTO.getComment().isEmpty()) {
            throw new NotFoundException(ErrorStatus.MISSING_COMMENT.getMessage());
        }

        // 게시글 ID 누락시 예외처리
        if (commentCreateDTO.getMemoId() == null) {
            throw new NotFoundException(ErrorStatus.MISSING_COMMENT_MEMOID.getMessage());
        }

        Long userId = memberService.getUserIdByEmail(userDetails.getUsername());
        commentService.createComment(commentCreateDTO, userId);

        return ApiResponse.success_only(SuccessStatus.CREATE_COMMENT_SUCCESS);
    }

    @Operation(
            summary = "댓글 조회 API",
            description = "메모에 달린 댓글을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "게시글 ID가 입력되지 않았습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없습니다.")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponseDTO>>> getCommentsByArticleId(
            @RequestParam Long memoId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        // 메모 ID 누락시 예외처리
        if (memoId == null) {
            throw new NotFoundException(ErrorStatus.MISSING_COMMENT_MEMOID.getMessage());
        }

        List<CommentResponseDTO> comments = commentService.getCommentsByMemoId(memoId,userDetails);

        return ApiResponse.success(SuccessStatus.SEND_COMMENT_SUCCESS, comments);
    }

    @Operation(
            summary = "댓글 수정 API",
            description = "게시글에 달린 댓글을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "댓글 ID가 입력되지 않았습니다. / 댓글이 입력되지 않았습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "수정 권한이 없습니다.")
    })
    @PatchMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommentUpdateDTO commentUpdateDTO,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        //댓글 ID 누락시 예외처리
        if (commentId == null) {
            throw new NotFoundException(ErrorStatus.MISSING_COMMENT_ID.getMessage());
        }

        // Comment 누락시 예외처리
        if (commentUpdateDTO.getComment() == null || commentUpdateDTO.getComment().isEmpty()) {
            throw new NotFoundException(ErrorStatus.MISSING_COMMENT.getMessage());
        }

        Long userId = memberService.getUserIdByEmail(userDetails.getUsername());
        commentService.updateComment(commentId, commentUpdateDTO, userId);

        return ApiResponse.success_only(SuccessStatus.MODIFY_COMMENT_SUCCESS);
    }

    @Operation(
            summary = "댓글 삭제 API",
            description = "메모에 달린 댓글을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "댓글 ID가 입력되지 않았습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "40₩", description = "삭제 권한이 없습니다.")
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        //댓글 ID 누락시 예외처리
        if (commentId == null) {
            throw new NotFoundException(ErrorStatus.MISSING_COMMENT_ID.getMessage());
        }

        Long userId = memberService.getUserIdByEmail(userDetails.getUsername());
        commentService.deleteComment(commentId, userId);

        return ApiResponse.success_only(SuccessStatus.DELETE_COMMENT_SUCCESS);
    }

    @Operation(
            summary = "댓글 좋아요 토글 API",
            description = "특정 댓글에 좋아요를 누르거나 취소합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 게시글을 찾을 수 없습니다."),
    })
    @PostMapping("/like/{commentId}")
    public ResponseEntity<ApiResponse<Void>> toggleLike(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = memberService.getUserIdByEmail(userDetails.getUsername());
        commentService.toggleLike(commentId, userId);
        return ApiResponse.success_only(SuccessStatus.TOGGLE_LIKE_SUCCESS);
    }
}
