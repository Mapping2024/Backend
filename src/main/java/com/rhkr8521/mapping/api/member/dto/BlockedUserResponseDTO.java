package com.rhkr8521.mapping.api.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedUserResponseDTO {
    private Long userId;
    private String profileImage;
    private String nickname;
}