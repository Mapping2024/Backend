package com.rhkr8521.mapping.api.member.dto;


import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.member.entity.Role;
import lombok.Getter;

@Getter
public class UserInfoResponseDTO {
    private final Long id;
    private final String nickname;
    private final String imageUrl;
    private final Role role;
    private final String socialId;

    public UserInfoResponseDTO(Member member){
        this.id = member.getId();
        this.nickname = member.getNickname();
        this.imageUrl = member.getImageUrl();
        this.socialId = member.getSocialId();
        this.role = member.getRole();
    }
}
