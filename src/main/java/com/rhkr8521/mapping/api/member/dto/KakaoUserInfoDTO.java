package com.rhkr8521.mapping.api.member.dto;

import lombok.Data;

@Data
public class KakaoUserInfoDTO {
    private String id;            // 사용자 고유 ID
    private String email;         // 이메일
    private String nickname;      // 닉네임
    private String profileImage;  // 프로필 이미지 URL
}

