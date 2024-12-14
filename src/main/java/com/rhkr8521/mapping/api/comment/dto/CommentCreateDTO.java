package com.rhkr8521.mapping.api.comment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentCreateDTO {

    private String comment;
    private Long memoId;
    private int rating;
}
