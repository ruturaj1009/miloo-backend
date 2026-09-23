package com.miloo.modules.health.controller;

import com.miloo.common.dto.ApiResponse;
import com.miloo.common.exception.ApiException;
import com.miloo.common.security.SecurityUtils;
import com.miloo.modules.auth.constant.AuthConstants;
import com.miloo.modules.auth.dto.*;
import com.miloo.modules.auth.service.AuthService;
import com.miloo.modules.health.dto.HealthMetricsDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    @GetMapping("/check")
    public ApiResponse<HealthMetricsDto> healthCheck() {
        return ApiResponse.ok(
                HealthMetricsDto.builder()
                .status("UP")
                .message("Server is running")
                .version("1.0.0")
                .build()
        );
    }
}
