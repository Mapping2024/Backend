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
    SEND_MEMO_DETAIL_SUCCESS(HttpStatus.OK, "메모 상세 조회 성공"),

    DELETE_MEMO_SUCCESS(HttpStatus.OK, "메모 삭제 성공"),
    UPDATE_MEMO_SUCCESS(HttpStatus.OK,"메모 수정 성공"),
    TOGGLE_LIKE_SUCCESS(HttpStatus.OK, "좋아요 토글 성공"),
    TOGGLE_HATE_SUCCESS(HttpStatus.OK,"싫어요 토글 성공"),

    SEND_COMMENT_IDS_SUCCESS(HttpStatus.OK,"댓글 ID 목록 조회 성공"),
    SEND_COMMENT_DETAIL_SUCCESS(HttpStatus.OK,"댓글 상세 조회 성공"),
    MODIFY_COMMENT_SUCCESS(HttpStatus.OK,"댓글 수정 성공"),
    DELETE_COMMENT_SUCCESS(HttpStatus.OK,"댓글 삭제 상공"),

    REPORT_MEMO_SUCCESS(HttpStatus.OK,"메모 신고 성공"),
    BLOCK_USER_SUCCESS(HttpStatus.OK,"회원 차단 성공"),
    SEND_BLOCK_LIST_SUCCESS(HttpStatus.OK,"회원 차단 목록 조회 성공"),
    UNBLOCK_USER_SUCCESS(HttpStatus.OK,"회원 차단 해제 성공"),

    /**
     * 201
     */
    CREATE_MEMO_SUCCESS(HttpStatus.CREATED, "메모 생성 성공"),
    CREATE_COMMENT_SUCCESS(HttpStatus.CREATED,"댓글 생성 성공"),

    ;

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}