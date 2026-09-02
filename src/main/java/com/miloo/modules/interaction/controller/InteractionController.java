package com.miloo.modules.interaction.controller;

import com.miloo.common.security.SecurityUtils;
import com.miloo.modules.interaction.dto.MatchSummaryDto;
import com.miloo.modules.interaction.dto.SwipeRequestDto;
import com.miloo.modules.interaction.dto.SwipeResponseDto;
import com.miloo.modules.interaction.service.InteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/swipes")
    public ResponseEntity<SwipeResponseDto> recordSwipe(@Valid @RequestBody SwipeRequestDto request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        SwipeResponseDto response = interactionService.recordSwipe(currentUserId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/matches")
    public ResponseEntity<List<MatchSummaryDto>> getMatches() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        List<MatchSummaryDto> matches = interactionService.getMatches(currentUserId);
        return ResponseEntity.ok(matches);
    }

    @DeleteMapping("/matches/{matchId}")
    public ResponseEntity<Map<String, Object>> unmatch(@PathVariable("matchId") UUID matchId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        interactionService.unmatch(currentUserId, matchId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
