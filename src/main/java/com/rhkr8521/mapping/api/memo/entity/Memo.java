package com.rhkr8521.mapping.api.memo.entity;

import com.rhkr8521.mapping.api.member.entity.Member;
import com.rhkr8521.mapping.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "memo")
@Builder(toBuilder = true)
public class Memo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memo_id")
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private double lat;
    private double lng;
    private String category;
    private long likeCnt;
    private long hateCnt;
    private String createIp;
    private String lastModifyIp;
    private boolean secret;
    private boolean certified;
    private boolean modify;
    private boolean isHidden;
    private boolean isDeleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Member member;

    @OneToMany(mappedBy = "memo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemoImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "memo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemoLike> memoLikes = new ArrayList<>();

    @OneToMany(mappedBy = "memo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MemoHate> memoHates = new ArrayList<>();

    // 메모 이미지 추가
    public void addImages(List<String> imageUrls) {
        for (String url : imageUrls) {
            MemoImage image = MemoImage.builder()
                    .imageUrl(url)
                    .memo(this)
                    .build();
            this.images.add(image);
        }
    }

}
