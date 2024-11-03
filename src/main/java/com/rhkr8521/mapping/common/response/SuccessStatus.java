package com.rhkr8521.mapping.common.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum SuccessStatus {

    /**
     * 200
     */
    SEND_KAKAO_ACCESSTOKEN_SUCCESS(HttpStatus.OK,"카카오 엑세스토큰 발급 성공"),
    SEND_LOGIN_SUCCESS(HttpStatus.OK, "로그인 성공"),
    SEND_REISSUE_TOKEN_SUCCESS(HttpStatus.OK,"토큰 재발급 성공"),
    SEND_USERDETAIL_SUCCESS(HttpStatus.OK, "유저 정보 발송 성공"),

    UPDATE_PROFILE_IMAGE_SUCCESS(HttpStatus.OK, "프로필 사진 변경 성공"),
    UPDATE_NICKNAME_SUCCESS(HttpStatus.OK, "닉네임 변경 성공"),
    CHECK_NICKNAME_SUCCESS(HttpStatus.OK, "닉네임 사용 가능"),
    DELETE_MEMBER_SUCCESS(HttpStatus.OK, "회원 탈퇴 성공"),
    GET_USERINFO_SUCCESS(HttpStatus.OK,"사용자 정보 조회 성공"),

    SEND_TOTAL_MEMO_SUCCESS(HttpStatus.OK, "전체 메모 발송 성공"),

    /**
     * 201
     */
    CREATE_MEMO_SUCCESS(HttpStatus.CREATED, "메모 생성 성공"),

    ;

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}