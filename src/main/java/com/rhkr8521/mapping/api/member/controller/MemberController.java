package com.rhkr8521.mapping.api.member.controller;

import com.rhkr8521.mapping.api.member.dto.*;
import com.rhkr8521.mapping.api.member.jwt.service.JwtService;
import com.rhkr8521.mapping.api.member.service.AppleService;
import com.rhkr8521.mapping.api.member.service.MemberService;
import com.rhkr8521.mapping.api.member.service.OAuthService;
import com.rhkr8521.mapping.common.exception.BadRequestException;
import com.rhkr8521.mapping.common.exception.InternalServerException;
import com.rhkr8521.mapping.common.response.ApiResponse;
import com.rhkr8521.mapping.common.response.ErrorStatus;
import com.rhkr8521.mapping.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Member", description = "Member 관련 API 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/member")
public class MemberController {

    private final MemberService memberService;
    private final OAuthService oauthService;
    private final JwtService jwtService;

    @Hidden
    @Operation(
            summary = "[백엔드 용] 카카오 엑세스토큰 발급 API",
            description = "/oauth2/authorization/kakao 엔드포인트를 통해 엑세스토큰을 발급합니다."
    )
    @GetMapping("/accesstoken")
    public ResponseEntity<ApiResponse<String>> getKakaoAccessToken(@RequestParam("code") String code) {
        // 인가 코드를 통해 액세스 토큰 요청
        String kakaoAccessToken = oauthService.getKakaoAccessToken(code);
        return ApiResponse.success(SuccessStatus.SEND_KAKAO_ACCESSTOKEN_SUCCESS, kakaoAccessToken);
    }

    @Operation(
            summary = "카카오로그인 API",
            description = "카카오 엑세스토큰을 통해 사용자의 정보를 등록 및 토큰을 발급합니다. (ROLE -> 일반사용자 : USER, 관리자 : ADMIN)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "카카오 엑세스토큰이 입력되지 않았습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 엑세스토큰 입니다.")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginWithKakao(@RequestBody KakaoLoginRequestDTO kakaoLoginRequest) {
        // 카카오 엑세스토큰이 입력되지 않았을 경우 예외 처리
        if (kakaoLoginRequest == null || kakaoLoginRequest.getAccessToken() == null || kakaoLoginRequest.getAccessToken().isEmpty()) {
            throw new BadRequestException(ErrorStatus.MISSING_KAKAO_ACCESSTOKEN.getMessage());
        }

        Map<String, Object> response = memberService.loginWithKakao(kakaoLoginRequest.getAccessToken());
        return ApiResponse.success(SuccessStatus.SEND_LOGIN_SUCCESS, response);
    }

    @Hidden
    @Operation(
            summary = "Apple 로그인 API",
            description = "Apple Authorization Code를 통해 사용자의 정보를 등록 및 토큰을 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Apple Authorization Code가 입력되지 않았습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Apple 소셜 로그인 중 오류 발생")
    })
    @PostMapping("/apple-login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginWithApple(@RequestParam("code") String code) {
        if (code == null || code.isEmpty()) {
            throw new BadRequestException(ErrorStatus.MISSING_APPLE_AUTHORIZATION_CODE_EXCEPTION.getMessage());
        }

        Map<String, Object> response = memberService.loginWithApple(code);
        return ApiResponse.success(SuccessStatus.SEND_LOGIN_SUCCESS, response);
    }

    @Operation(
            summary = "토큰 재발급 API",
            description = "유효한 리프레시 토큰을 헤더(Authorization-Refresh)로 제공하면 새로운 액세스 토큰과 리프레시 토큰을 발급하여 헤더로 전송합니다. | [주의] 스웨거로 테스트할때 토큰 앞에 'Bearer '을 넣어야합니다. "
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "리프레시 토큰이 입력되지 않았습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰입니다."),
    })
    @GetMapping("/token-reissue")
    public ResponseEntity<ApiResponse<Void>> reissueToken(@RequestHeader(value = "Authorization-Refresh", required = false) String refreshToken) {
        // 리프레시 토큰이 입력되지 않았을 경우 예외 처리
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new BadRequestException(ErrorStatus.MISSING_REFRESHTOKEN.getMessage());
        }

        // 리프레시 토큰의 유효성을 검사
        if (!jwtService.isTokenValid(refreshToken.substring(7))) {
            throw new BadRequestException(ErrorStatus.INVALID_REFRESHTOKEN_EXCEPTION.getMessage()); // 유효하지 않은 토큰에 대한 예외 처리
        }

        return ApiResponse.success_only(SuccessStatus.SEND_REISSUE_TOKEN_SUCCESS);
    }

    @Operation(
            summary = "닉네임 변경 API",
            description = "사용자의 닉네임을 변경합니다. (닉네임 필터 조건 : 닉네임은 10자 이하로 설정, 닉네임은 영문, 숫자, 한글만 사용가능, 현재 다른 사용자가 사용중인 닉네임은 사용 불가)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "닉네임 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "닉네임이 입려되지 않았습니다."),
    })
    @PatchMapping("/modify-nickname")
    public ResponseEntity<ApiResponse<Void>> changeNickname(@AuthenticationPrincipal UserDetails userDetails,
                                                            @RequestParam("nickname") String nickname) {
        Long userId = memberService.getUserIdByEmail(userDetails.getUsername());

        // 닉네임이 입력되지 않았을 경우 예외 처리
        if (nickname == null || nickname.isEmpty()) {
            throw new BadRequestException(ErrorStatus.MISSING_NICKNAME.getMessage());
        }

        memberService.changeNickname(userId, nickname);
        return ApiResponse.success_only(SuccessStatus.UPDATE_NICKNAME_SUCCESS);
    }

    @Operation(
            summary = "프로필 사진 변경 API",
            description = "사용자의 프로필 사진을 변경합니다. with MultipartFile"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 사진 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "수정할 프로필 이미지파일이 업로드 되지 않았습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "프로필 사진이 변경되지 않았습니다.")
    })
    @PatchMapping(value = "/modify-profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> changeProfileImage(@AuthenticationPrincipal UserDetails userDetails,
                                                                @RequestParam("image") MultipartFile image) {
        Long userId = memberService.getUserIdByEmail(userDetails.getUsername());

        // 이미지 파일 검증
        if (image != null && !image.isEmpty()) {
            if (!isImageFile(image)) {
                throw new BadRequestException(ErrorStatus.NOT_ALLOW_IMG_MIME.getMessage());
            }
        }else{
            throw new BadRequestException(ErrorStatus.MISSING_UPLOAD_IMAGE.getMessage());
        }

        try {
            memberService.updateProfileImage(userId, image);
            return ApiResponse.success_only(SuccessStatus.UPDATE_PROFILE_IMAGE_SUCCESS);
        } catch (IOException e) {
            throw new InternalServerException(ErrorStatus.FAIL_UPLOAD_PROFILE_IMAGE.getMessage());
        }
    }

    @Operation(
            summary = "사용자 정보 조회 API",
            description = "토큰을 통해 인증된 사용자의 정보를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다.")
    })
    @GetMapping("/user-info")
    public ResponseEntity<ApiResponse<UserInfoResponseDTO>> getUserInfo(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = memberService.getUserIdByEmail(userDetails.getUsername());
        UserInfoResponseDTO userInfo = memberService.getUserInfo(userId);
        return ApiResponse.success(SuccessStatus.GET_USERINFO_SUCCESS, userInfo);
    }

    @Operation(
            summary = "회원 탈퇴 API",
            description = "로그인한 사용자의 계정을 논리적 삭제 처리합니다. (기존 작성된 메모, 댓글 등은 유지됩니다.)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 탈퇴한 회원입니다.")
    })
    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdrawMember(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = memberService.getUserIdByEmail(userDetails.getUsername());
        memberService.withdrawMember(userId);
        return ApiResponse.success_only(SuccessStatus.DELETE_MEMBER_SUCCESS);
    }

    @Operation(
            summary = "유저 차단 API",
            description = "특정 사용자를 차단합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원 차단 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다."),
    })
    @PostMapping("/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(@AuthenticationPrincipal UserDetails userDetails,
                                                       @RequestParam("userId") Long userId) {
        Long blockerId = memberService.getUserIdByEmail(userDetails.getUsername());
        memberService.blockUser(blockerId, userId);
        return ApiResponse.success_only(SuccessStatus.BLOCK_USER_SUCCESS);
    }

    @Operation(
            summary = "차단한 유저 목록 조회 API",
            description = "내가 차단한 사용자 목록을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원 차단 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다."),
    })
    @GetMapping("/block/list")
    public ResponseEntity<ApiResponse<List<BlockedUserResponseDTO>>> getBlockedUsers(@AuthenticationPrincipal UserDetails userDetails) {
        Long blockerId = memberService.getUserIdByEmail(userDetails.getUsername());
        List<BlockedUserResponseDTO> response = memberService.getBlockedUserResponseList(blockerId);
        return ApiResponse.success(SuccessStatus.SEND_BLOCK_LIST_SUCCESS, response);
    }

    @Operation(
            summary = "유저 차단 해제 API",
            description = "차단한 사용자 중 특정 사용자의 차단을 해제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원 차단 해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다."),
    })
    @DeleteMapping("/block/{userId}")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@AuthenticationPrincipal UserDetails userDetails,
                                                         @PathVariable Long userId) {
        Long blockerId = memberService.getUserIdByEmail(userDetails.getUsername());
        memberService.unblockUser(blockerId, userId);
        return ApiResponse.success_only(SuccessStatus.UNBLOCK_USER_SUCCESS);
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
}
