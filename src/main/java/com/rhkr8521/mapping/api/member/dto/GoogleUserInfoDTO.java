package com.rhkr8521.mapping.api.member.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoogleUserInfoDTO {
    private final String id;            // socialId 로 사용할 Google sub
    private final String email;         // 사용자 이메일
    private final String refreshToken;  // Google refresh_token
}
