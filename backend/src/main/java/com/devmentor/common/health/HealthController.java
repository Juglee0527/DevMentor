package com.devmentor.common.health;

import com.devmentor.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(
                "DevMentor API가 정상적으로 실행 중입니다.",
                new HealthResponse("UP")
        );
    }
}

