package com.rhkr8521.mapping.api.memo.controller;

import com.rhkr8521.mapping.api.member.service.MemberService;
import com.rhkr8521.mapping.api.memo.dto.MemoCreateRequestDTO;
import com.rhkr8521.mapping.api.memo.service.MemoService;
import com.rhkr8521.mapping.common.exception.BadRequestException;
import com.rhkr8521.mapping.common.response.ApiResponse;
import com.rhkr8521.mapping.common.response.ErrorStatus;
import com.rhkr8521.mapping.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "Memo", description = "Memo 관련 API 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/memo")
public class MemoController {

    private final MemoService memoService;
    private final MemberService memberService;

    @Operation(
            summary = "메모 등록 API",
            description = "새로운 메모를 등록합니다. with MultipartFile"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "매모 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = ""),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "")
    })
    @PostMapping(value = "/new", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> createMemo(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("lat") String lat,
            @RequestParam("lng") String lng,
            @RequestParam("category") String category,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            HttpServletRequest request) throws IOException {

        // 필수 입력 값 누락 체크
        if (isNullOrEmpty(title) ||
                isNullOrEmpty(content) ||
                isNullOrEmpty(lat) ||
                isNullOrEmpty(lng) ||
                isNullOrEmpty(category)) {
            throw new BadRequestException(ErrorStatus.VALIDATION_CONTENT_MISSING_EXCEPTION.getMessage());
        }

        // 이미지 파일 검증
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                if (!isImageFile(image)) {
                    throw new BadRequestException(ErrorStatus.NOT_ALLOW_IMG_MIME.getMessage());
                }
            }
        }

        MemoCreateRequestDTO memoCreateRequestDTO = MemoCreateRequestDTO.builder()
                .title(title)
                .content(content)
                .lat(lat)
                .lng(lng)
                .category(category)
                .build();

        Long userId = memberService.getUserIdByEmail(userDetails.getUsername());

        memoService.createMemo(userId, memoCreateRequestDTO, images, request);
        return ApiResponse.success_only(SuccessStatus.CREATE_MEMO_SUCCESS);
    }

    private boolean isImageFile(MultipartFile file) {
        // 허용되는 이미지 MIME 타입
        String contentType = file.getContentType();
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/jpg") ||
                        contentType.equals("image/bmp") ||
                        contentType.equals("image/webp")
        );
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

}
