package com.miloo.modules.people.controller;

import com.miloo.common.security.SecurityUtils;
import com.miloo.modules.people.dto.ProfileMediaDto;
import com.miloo.modules.people.dto.UpdateProfileRequestDto;
import com.miloo.modules.people.dto.UserProfileDto;
import com.miloo.modules.people.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMyProfile() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UserProfileDto profile = profileService.getMyProfile(currentUserId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/me")
    public ResponseEntity<UserProfileDto> updateMyProfile(@RequestBody UpdateProfileRequestDto request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UserProfileDto profile = profileService.updateMyProfile(currentUserId, request);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileDto> getProfileById(@PathVariable("userId") UUID userId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UserProfileDto profile = profileService.getProfileById(userId, currentUserId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/me/photos")
    public ResponseEntity<ProfileMediaDto> addPhoto(@RequestBody ProfileMediaDto photoDto) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        ProfileMediaDto created = profileService.addPhoto(currentUserId, photoDto);
        return ResponseEntity.ok(created);
    }

    @DeleteMapping("/me/photos/{mediaId}")
    public ResponseEntity<Map<String, Object>> deletePhoto(@PathVariable("mediaId") UUID mediaId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        profileService.deletePhoto(currentUserId, mediaId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
