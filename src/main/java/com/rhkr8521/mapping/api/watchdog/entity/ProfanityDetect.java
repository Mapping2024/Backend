package com.rhkr8521.mapping.api.watchdog.entity;

import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "profanity_detect")
@Builder(toBuilder = true)
public class ProfanityDetect extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profanity_detect_id")
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String detectedWords;

    @Column(columnDefinition = "TEXT")
    private String originalText;

    @Column(columnDefinition = "TEXT")
    private String censoredText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}
