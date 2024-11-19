package com.rhkr8521.mapping.api.member.dto;


import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.member.entity.Role;
import lombok.Getter;

@Getter
public class UserInfoResponseDTO {
    private final String nickname;
    private final String profileImage;
    private final Role role;
    private final String socialId;

    public UserInfoResponseDTO(Member member){
        this.nickname = member.getNickname();
        this.profileImage = member.getImageUrl();
        this.socialId = member.getSocialId();
        this.role = member.getRole();
    }
}
