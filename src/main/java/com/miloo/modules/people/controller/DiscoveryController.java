package com.miloo.modules.people.controller;

import com.miloo.common.security.SecurityUtils;
import com.miloo.modules.people.dto.UserProfileDto;
import com.miloo.modules.people.service.DiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/discovery")
@RequiredArgsConstructor
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    @GetMapping("/feed")
    public ResponseEntity<List<UserProfileDto>> getDiscoveryFeed(
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "max_distance_km", required = false, defaultValue = "50") Integer maxDistanceKm,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit,
            @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        List<UserProfileDto> feed = discoveryService.getDiscoveryFeed(
                currentUserId, latitude, longitude, maxDistanceKm, limit, offset
        );
        return ResponseEntity.ok(feed);
    }
}
