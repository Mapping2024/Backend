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
public class MemoDetailResponseDTO {
    private Long id;
    private String title;
    private String content;
    private String date;
    private double lat;
    private double lng;
    private String category;
    private long likeCnt;
    private long hateCnt;
    private List<String> images;
    private boolean myMemo;
    private boolean myLike;
    private boolean myHate;
    private boolean certified;
    private boolean modify;
    private Long authorId;
    private String nickname;
    private String profileImage;
}
