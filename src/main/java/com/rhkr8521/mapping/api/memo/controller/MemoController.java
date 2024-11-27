package com.rhkr8521.mapping.api.memo.controller;

import com.rhkr8521.mapping.api.member.service.MemberService;
import com.rhkr8521.mapping.api.memo.dto.MemoCreateRequestDTO;
import com.rhkr8521.mapping.api.memo.dto.MemoDetailResponseDTO;
import com.rhkr8521.mapping.api.memo.dto.MemoTotalListResponseDTO;
import com.rhkr8521.mapping.api.memo.dto.MyMemoListResponseDTO;
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

    @Operation(
            summary = "메모 상세 조회 API",
            description = "특정 메모의 상세 정보를 조회합니다. / 비 로그인 상태이면 토큰을 안넘기고, 로그인상태이면 엑세스토큰을 넘겨줘야합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메모 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필수 정보가 입력되지 않았습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 메모를 찾을 수 없습니다."),
    })
    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<MemoDetailResponseDTO>> getMemoDetail(
            @RequestParam Long memoId,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 필수 입력 값 누락 체크
        if (memoId == null) {
            throw new BadRequestException(ErrorStatus.VALIDATION_CONTENT_MISSING_EXCEPTION.getMessage());
        }

        MemoDetailResponseDTO memoDetail = memoService.getMemoDetail(memoId, userDetails);
        return ApiResponse.success(SuccessStatus.SEND_MEMO_DETAIL_SUCCESS, memoDetail);
    }

    @Operation(
            summary = "내 메모 조회 API",
            description = "내가 작성한 메모를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메모 조회 성공"),
    })
    @GetMapping("/my-memo")
    public ResponseEntity<ApiResponse<List<MyMemoListResponseDTO>>> getMyMemo(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<MyMemoListResponseDTO> myMemoList = memoService.getMyMemoList(userDetails);
        return ApiResponse.success(SuccessStatus.SEND_TOTAL_MEMO_SUCCESS, myMemoList);
    }

    @Operation(
            summary = "메모 삭제 API",
            description = "등록한 메모를 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메모 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "메모 작성자와 삭제 요청자가 다릅니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "메모를 찾을 수 없습니다.")
    })
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMemo(
            @PathVariable Long memoId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        // 필수 입력 값 누락 체크
        if (memoId == null) {
            throw new BadRequestException(ErrorStatus.VALIDATION_CONTENT_MISSING_EXCEPTION.getMessage());
        }

        Long userId = memberService.getUserIdByEmail(userDetails.getUsername());
        memoService.deleteMemo(memoId, userId);

        return ApiResponse.success_only(SuccessStatus.DELETE_MEMO_SUCCESS);
    }

    @Operation(
            summary = "메모 수정 API",
            description = "기존 메모를 수정합니다. 삭제할 이미지 URL과 새로운 이미지를 함께 처리합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메모 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필수 정보가 입력되지 않았습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 메모를 찾을 수 없습니다."),
    })
    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateMemo(
            @PathVariable Long memoId,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("category") String category,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "deleteImageUrls", required = false) List<String> deleteImageUrls,
            HttpServletRequest request
    ) throws IOException {

        // 필수 입력 값 누락 체크
        if (isNullOrEmpty(title) ||
                isNullOrEmpty(content) ||
                isNullOrEmpty(category) ||
                memoId == null) {
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

        MemoCreateRequestDTO memoUpdateRequestDTO = MemoCreateRequestDTO.builder()
                .title(title)
                .content(content)
                .category(category)
                .build();

        Long userId = memberService.getUserIdByEmail(userDetails.getUsername());

        memoService.updateMemo(memoId, userId, memoUpdateRequestDTO, images, deleteImageUrls);

        return ApiResponse.success_only(SuccessStatus.UPDATE_MEMO_SUCCESS);
    }

    @Operation(
            summary = "메모 좋아요 토글 API",
            description = "특정 메모에 좋아요를 누르거나 취소합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 메모를 찾을 수 없습니다."),
    })
    @PostMapping("/like/{memoId}")
    public ResponseEntity<ApiResponse<Void>> toggleLike(
            @PathVariable Long memoId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = memberService.getUserIdByEmail(userDetails.getUsername());
        memoService.toggleLike(memoId, userId);
        return ApiResponse.success_only(SuccessStatus.TOGGLE_LIKE_SUCCESS);
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
