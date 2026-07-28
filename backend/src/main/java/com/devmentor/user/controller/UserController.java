package com.devmentor.user.controller;

import com.devmentor.common.response.ApiResponse;
import com.devmentor.user.dto.UserRequest;
import com.devmentor.user.dto.UserResponse;
import com.devmentor.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ApiResponse.success("사용자를 생성했습니다.", userService.create(request));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> get(@PathVariable Long userId) {
        return ApiResponse.success("사용자를 조회했습니다.", userService.get(userId));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> update(
            @PathVariable Long userId,
            @Valid @RequestBody UserRequest request
    ) {
        return ApiResponse.success("사용자를 수정했습니다.", userService.update(userId, request));
    }
}
