package com.rhkr8521.mapping.api.memo.dto;

import com.rhkr8521.mapping.api.memo.entity.ReportReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoReportRequestDTO {
    private Long memoId;
    private ReportReason reportReason;
}