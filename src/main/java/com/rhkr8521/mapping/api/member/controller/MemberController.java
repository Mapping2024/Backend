package com.rhkr8521.mapping.api.member.controller;

import com.rhkr8521.mapping.api.member.dto.*;
import com.rhkr8521.mapping.api.member.jwt.service.JwtService;
import com.rhkr8521.mapping.api.member.service.MemberService;
import com.rhkr8521.mapping.api.member.service.OAuthService;
import com.rhkr8521.mapping.common.exception.BadRequestException;
import com.rhkr8521.mapping.common.response.ApiResponse;
import com.rhkr8521.mapping.common.response.ErrorStatus;
import com.rhkr8521.mapping.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Member", description = "Member 관련 API 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/member")
public class MemberController {

    private final MemberService memberService;
    private final OAuthService oauthService;
    private final JwtService jwtService;

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
            summary = "로그인 API",
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
}
