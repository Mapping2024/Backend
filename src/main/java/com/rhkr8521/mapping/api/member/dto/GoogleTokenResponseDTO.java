package com.rhkr8521.mapping.api.member.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonDeserialize(builder = GoogleTokenResponseDTO.GoogleTokenResponseDTOBuilder.class)
public class GoogleTokenResponseDTO {
    private final String access_token;
    private final String refresh_token;

    @JsonPOJOBuilder(withPrefix = "")
    public static class GoogleTokenResponseDTOBuilder { }
}
