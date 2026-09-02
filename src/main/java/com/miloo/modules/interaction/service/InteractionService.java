package com.miloo.modules.interaction.service;

import com.miloo.common.exception.ApiException;
import com.miloo.modules.interaction.dto.MatchSummaryDto;
import com.miloo.modules.interaction.dto.SwipeRequestDto;
import com.miloo.modules.interaction.dto.SwipeResponseDto;
import com.miloo.modules.interaction.entity.MatchEntity;
import com.miloo.modules.interaction.entity.SwipeEntity;
import com.miloo.modules.interaction.repository.MatchRepository;
import com.miloo.modules.interaction.repository.SwipeRepository;
import com.miloo.modules.people.dto.UserProfileDto;
import com.miloo.modules.people.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionService {

    private final SwipeRepository swipeRepository;
    private final MatchRepository matchRepository;
    private final ProfileService profileService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public SwipeResponseDto recordSwipe(UUID currentUserId, SwipeRequestDto req) {
        if (currentUserId.equals(req.getTargetUserId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot swipe on yourself");
        }

        SwipeEntity swipe = swipeRepository.findBySwiperIdAndTargetId(currentUserId, req.getTargetUserId())
                .orElseGet(() -> SwipeEntity.builder()
                        .swiperId(currentUserId)
                        .targetId(req.getTargetUserId())
                        .build());

        swipe.setAction(req.getAction().toUpperCase());
        SwipeEntity savedSwipe = swipeRepository.save(swipe);

        boolean isPositiveSwipe = "LIKE".equalsIgnoreCase(req.getAction()) || "SUPERLIKE".equalsIgnoreCase(req.getAction());

        if (isPositiveSwipe) {
            boolean reciprocal = swipeRepository.existsBySwiperIdAndTargetIdAndActionIn(
                    req.getTargetUserId(),
                    currentUserId,
                    List.of("LIKE", "SUPERLIKE")
            );

            if (reciprocal) {
                log.info("[Match Engine] Reciprocal match between {} and {}", currentUserId, req.getTargetUserId());

                UUID u1 = currentUserId.compareTo(req.getTargetUserId()) < 0 ? currentUserId : req.getTargetUserId();
                UUID u2 = currentUserId.compareTo(req.getTargetUserId()) < 0 ? req.getTargetUserId() : currentUserId;

                MatchEntity match = matchRepository.findActiveMatchBetween(u1, u2)
                        .orElseGet(() -> matchRepository.save(MatchEntity.builder()
                                .userOneId(u1)
                                .userTwoId(u2)
                                .isActive(true)
                                .build()));

                UserProfileDto partnerProfile = profileService.getProfileById(req.getTargetUserId(), currentUserId);
                UserProfileDto myProfile = profileService.getProfileById(currentUserId, req.getTargetUserId());

                MatchSummaryDto matchForCurrent = MatchSummaryDto.builder()
                        .matchId(match.getMatchId())
                        .userOneId(match.getUserOneId())
                        .userTwoId(match.getUserTwoId())
                        .partnerProfile(partnerProfile)
                        .unreadCount(0)
                        .isActive(true)
                        .createdAt(match.getCreatedAt() != null ? match.getCreatedAt().toString() : null)
                        .build();

                MatchSummaryDto matchForTarget = MatchSummaryDto.builder()
                        .matchId(match.getMatchId())
                        .userOneId(match.getUserOneId())
                        .userTwoId(match.getUserTwoId())
                        .partnerProfile(myProfile)
                        .unreadCount(0)
                        .isActive(true)
                        .createdAt(match.getCreatedAt() != null ? match.getCreatedAt().toString() : null)
                        .build();

                // Push real-time notification to both users via STOMP /user/queue/matches
                try {
                    messagingTemplate.convertAndSendToUser(req.getTargetUserId().toString(), "/queue/matches", matchForTarget);
                    messagingTemplate.convertAndSendToUser(currentUserId.toString(), "/queue/matches", matchForCurrent);
                } catch (Exception ex) {
                    log.warn("Could not push match notification over STOMP broker: {}", ex.getMessage());
                }

                return SwipeResponseDto.builder()
                        .swipeId(savedSwipe.getSwipeId())
                        .action(savedSwipe.getAction())
                        .isMatch(true)
                        .match(matchForCurrent)
                        .build();
            }
        }

        return SwipeResponseDto.builder()
                .swipeId(savedSwipe.getSwipeId())
                .action(savedSwipe.getAction())
                .isMatch(false)
                .build();
    }

    @Transactional(readOnly = true)
    public List<MatchSummaryDto> getMatches(UUID currentUserId) {
        List<MatchEntity> matches = matchRepository.findActiveMatchesByUserId(currentUserId);

        return matches.stream().map(m -> {
            UUID partnerId = m.getUserOneId().equals(currentUserId) ? m.getUserTwoId() : m.getUserOneId();
            UserProfileDto partnerProfile = profileService.getProfileById(partnerId, currentUserId);

            return MatchSummaryDto.builder()
                    .matchId(m.getMatchId())
                    .userOneId(m.getUserOneId())
                    .userTwoId(m.getUserTwoId())
                    .partnerProfile(partnerProfile)
                    .unreadCount(0)
                    .isActive(m.getIsActive())
                    .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().toString() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public void unmatch(UUID currentUserId, UUID matchId) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Match not found"));

        if (!match.getUserOneId().equals(currentUserId) && !match.getUserTwoId().equals(currentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not authorized to unmatch this user");
        }

        match.setIsActive(false);
        matchRepository.save(match);
    }
}
