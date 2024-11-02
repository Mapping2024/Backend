package com.rhkr8521.mapping.api.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {

    ADMIN("ROLE_ADMIN"), USER("ROLE_USER"), AD("ROLE_AD");

    private final String key;
}
