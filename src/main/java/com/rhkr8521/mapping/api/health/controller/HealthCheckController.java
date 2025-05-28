package com.rhkr8521.mapping.api.health.controller;

import com.rhkr8521.mapping.common.response.ApiResponse;
import com.rhkr8521.mapping.common.response.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "서버 상태 관련 API 입니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/health")
public class HealthCheckController {

    @Operation(
            summary = "Health 체크 API",
            description = "서버가 정상인지 체크하는 API 입니다."
    )
    @GetMapping()
    public ResponseEntity<ApiResponse<Void>> getHealthCheck() {
        return ApiResponse.success_only(SuccessStatus.SEND_SERVER_OK);
    }
}
