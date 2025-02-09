package com.rhkr8521.mapping.api.memo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class MemoListResponseDTO {
    private Long id;          // 메모 ID
    private String title;     // 제목
    private String content;   // 내용
    private String category;  // 카테고리
    private long likeCnt;     // 좋아요 개수
    private long hateCnt;     // 싫어요 개수
    private List<String> images; // 이미지 목록
}
