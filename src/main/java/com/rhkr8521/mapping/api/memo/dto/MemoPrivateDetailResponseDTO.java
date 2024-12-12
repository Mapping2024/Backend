package com.rhkr8521.mapping.api.memo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoPrivateDetailResponseDTO {
    private Long id;
    private String title;
    private String content;
    private String date;
    private double lat;
    private double lng;
    private String category;
    private List<String> images;
    private Long authorId;
    private String nickname;
    private String profileImage;
}
