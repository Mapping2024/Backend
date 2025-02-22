package com.rhkr8521.mapping.api.report.entity;

import lombok.Getter;

@Getter
public enum ReportReason {
    SPAM("스팸홍보/도배글입니다."),
    OBSCENE("음란물입니다."),
    ILLEGAL_INFORMATION("불법정보를 포함하고 있습니다."),
    HARMFUL_TO_MINORS("청소년에게 유해한 내용입니다."),
    OFFENSIVE_EXPRESSION("욕설/생명경시/혐오/차벌적 표현입니다."),
    PRIVACY_EXPOSURE("개인정보 노출 게시물입니다."),
    UNPLEASANT_EXPRESSION("불쾌한 표현이 있습니다."),
    OTHER("기타");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }
}