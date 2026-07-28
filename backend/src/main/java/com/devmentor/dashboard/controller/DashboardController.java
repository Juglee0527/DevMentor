package com.devmentor.dashboard.controller;

import com.devmentor.common.response.ApiResponse;
import com.devmentor.dashboard.dto.DashboardResponse;
import com.devmentor.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ApiResponse<DashboardResponse> getDashboard(@RequestParam Long userId) {
        return ApiResponse.success(
                "대시보드를 조회했습니다.",
                dashboardService.getDashboard(userId)
        );
    }
}
