package com.rhkr8521.mapping.api.comment.entity;

import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.api.memo.entity.Memo;
import com.rhkr8521.mapping.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "comment")
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private int rating;
    private int likeCnt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memo_id")
    private Memo memo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Member member;

    // 좋아요 증가
    public Comment increaseLikeCnt() {
        return this.toBuilder()
                .likeCnt(this.likeCnt + 1)
                .build();
    }

    // 좋아요 감소
    public Comment decreaseLikeCnt() {
        return this.toBuilder()
                .likeCnt(this.likeCnt - 1)
                .build();
    }
}
