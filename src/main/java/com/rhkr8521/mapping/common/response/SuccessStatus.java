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
    SET_USER_MARKETING_SUCCESS(HttpStatus.OK, "유저 마케팅 동의 여부 설정 성공"),

    SEND_QUESTION_SUCCESS(HttpStatus.OK, "문제 발송 성공"),

    UPDATE_PROFILE_IMAGE_SUCCESS(HttpStatus.OK, "프로필 사진 변경 성공"),
    UPDATE_NICKNAME_SUCCESS(HttpStatus.OK, "닉네임 변경 성공"),
    CHECK_NICKNAME_SUCCESS(HttpStatus.OK, "닉네임 사용 가능"),
    UPDATE_INFO_OPEN_SUCCESS(HttpStatus.OK,"정보 공개 수정 성공"),
    DELETE_MEMBER_SUCCESS(HttpStatus.OK, "회원 탈퇴 성공"),
    GET_USERINFO_SUCCESS(HttpStatus.OK,"사용자 정보 조회 성공"),
    USER_FOLLOW_SUCCESS(HttpStatus.OK,"팔로우 성공"),
    USER_UNFOLLOW_SUCCESS(HttpStatus.OK,"언팔로우 성공"),
    GET_FOLLOWED_USERS_SUCCESS(HttpStatus.OK,"팔로우 중인 사용자 목록 조회 성공"),
    GET_FOLLOWER_USERS_SUCCESS(HttpStatus.OK, "팔로워 사용자 목록 조회 성공"),

    BOOK_SEARCH_SUCCESS(HttpStatus.OK, "책 결과 반환 성공"),

    GET_BOOKSHELF_SUCCESS(HttpStatus.OK,"책장 조회 성공"),
    GET_BOOKSHELF_INFO_SUCCESS(HttpStatus.OK,"책장 상세 정보 조회 성공"),
    UPDATE_BOOKSHELF_INFO_SUCCESS(HttpStatus.OK,"책장 상세 정보 수정 성공"),
    DELETE_BOOKSHELF_SUCCESS(HttpStatus.OK,"책장 삭제 성공"),
    SHIFT_BOOKSHELF_SUCCESS(HttpStatus.OK,"책장 이동 성공"),

    GET_ARTICLE_SUCCESS(HttpStatus.OK,"게시글 조회 성공"),
    GET_ARTICLE_LIST_SUCCESS(HttpStatus.OK,"게시글 목록 조회 성공"),
    MODIFY_ARTICLE_SUCCESS(HttpStatus.OK, "게시글 수정 성공"),
    DELETE_ARTICLE_SUCCESS(HttpStatus.OK, "게시글 삭제 성공"),

    GET_COMMENT_SUCCESS(HttpStatus.OK, "댓글 조회 성공"),
    MODIFY_COMMENT_SUCCESS(HttpStatus.OK,"댓글 수정 성공"),
    DELETE_COMMENT_SUCCESS(HttpStatus.OK,"댓글 삭제 성공"),

    /**
     * 201
     */
    CREATE_ARTICLE_SUCCESS(HttpStatus.CREATED, "게시판 등록 성공"),
    CREATE_USERTAG_SUCCESS(HttpStatus.CREATED, "USERTAG 등록 성공"),
    CREATE_BOOKSHELF_SUCCESS(HttpStatus.CREATED, "책장 등록 성공"),
    CREATE_COMMENT_SUCCESS(HttpStatus.CREATED, "댓글 등록 성공"),

    ;

    private final HttpStatus httpStatus;
    private final String message;

    public int getStatusCode() {
        return this.httpStatus.value();
    }
}