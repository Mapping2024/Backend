package com.rhkr8521.mapping.api.memo.controller;

import com.rhkr8521.mapping.api.member.service.MemberService;
import com.rhkr8521.mapping.api.memo.dto.MemoCreateRequestDTO;
import com.rhkr8521.mapping.api.memo.dto.MemoTotalListResponseDTO;
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필수 정보가 입력되지 않았습니다."),
    })
    @PostMapping(value = "/new", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> createMemo(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng,
            @RequestParam("category") String category,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            HttpServletRequest request) throws IOException {

        // 필수 입력 값 누락 체크
        if (isNullOrEmpty(title) ||
                isNullOrEmpty(content) ||
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

    @Operation(
            summary = "전체 메모 조회 API",
            description = "현재 위치 위도와 경도를 기준으로 km 반경 내의 메모를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메모 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필수 정보가 입력되지 않았습니다."),
    })
    @GetMapping("/total")
    public ResponseEntity<ApiResponse<List<MemoTotalListResponseDTO>>> getMemosWithinRadius(
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng,
            @RequestParam("km") Double km) {

        // 필수 입력 값 누락 체크
        if (lat == null || lng == null || km == null) {
            throw new BadRequestException(ErrorStatus.VALIDATION_CONTENT_MISSING_EXCEPTION.getMessage());
        }

        List<MemoTotalListResponseDTO> memos = memoService.getMemosWithinRadius(lat, lng, km);
        return ApiResponse.success(SuccessStatus.SEND_TOTAL_MEMO_SUCCESS, memos);
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
