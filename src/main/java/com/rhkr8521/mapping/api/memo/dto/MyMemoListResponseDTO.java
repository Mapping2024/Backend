package com.rhkr8521.mapping.api.memo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MyMemoListResponseDTO {

    private Long id;
    private String title;
    private String content;
    private String category;
    private long likeCnt;
    private long hateCnt;
    private List<String> images;
}
