package com.rhkr8521.mapping.api.memo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoCreateRequestDTO {
    private String title;
    private String content;
    private double lat;
    private double lng;
    private double currentLat;
    private double currentLng;
    private String category;
    private boolean secret;
}
