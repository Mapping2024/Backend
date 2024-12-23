package com.rhkr8521.mapping.api.comment.dto;

import com.rhkr8521.mapping.api.comment.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class CommentResponseDTO {

    private Long id;
    private String comment;
    private int rating;
    private int likeCnt;
    private boolean modify;
    private String nickname;
    private String profileImageUrl;
    private String updatedAt;
    private boolean myLike;

    public static CommentResponseDTO fromEntity(Comment comment, boolean myLike) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return CommentResponseDTO.builder()
                .id(comment.getId())
                .comment(comment.getComment())
                .rating(comment.getRating())
                .likeCnt(comment.getLikeCnt())
                .nickname(comment.getMember().getNickname())
                .profileImageUrl(comment.getMember().getImageUrl())
                .updatedAt(comment.getCreatedAt().format(dateTimeFormatter))
                .myLike(myLike)
                .modify(comment.isModify())
                .build();
    }
}