package com.rhkr8521.mapping.api.memo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemoTotalListResponseDTO {
    private Long id;
    private String title;
    private String category;
    private double lat;
    private double lng;
    private boolean certified;
    private boolean secret;
}
