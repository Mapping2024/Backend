package com.rhkr8521.mapping.api.comment.dto;

import com.rhkr8521.mapping.api.comment.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class CommentResponseDTO {

    private Long id;
    private Long writerId;
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

        // 삭제된 댓글인 경우 처리
        if(comment.isDeleted()) {
            return CommentResponseDTO.builder()
                    .id(comment.getId())
                    .writerId(comment.getMember().getId())
                    .comment("삭제된 댓글입니다.")
                    .rating(comment.getRating())
                    .likeCnt(comment.getLikeCnt())
                    .nickname("(알수없음)")
                    .profileImageUrl(null)
                    .updatedAt(comment.getCreatedAt().format(dateTimeFormatter))
                    .myLike(false)
                    .modify(comment.isModify())
                    .build();
        }

        // 삭제되지 않은 일반 댓글의 경우
        String nickname = comment.getMember().isDeleted() ? "(알수없음)" : comment.getMember().getNickname();
        return CommentResponseDTO.builder()
                .id(comment.getId())
                .writerId(comment.getMember().getId())
                .comment(comment.getComment())
                .rating(comment.getRating())
                .likeCnt(comment.getLikeCnt())
                .nickname(nickname)
                .profileImageUrl(comment.getMember().getImageUrl())
                .updatedAt(comment.getCreatedAt().format(dateTimeFormatter))
                .myLike(myLike)
                .modify(comment.isModify())
                .build();
    }
}