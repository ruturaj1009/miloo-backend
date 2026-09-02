package com.miloo.modules.interaction.service;

import com.miloo.modules.interaction.dto.MatchSummaryDto;
import com.miloo.modules.interaction.dto.SwipeRequestDto;
import com.miloo.modules.interaction.dto.SwipeResponseDto;
import com.miloo.modules.interaction.entity.MatchEntity;
import com.miloo.modules.interaction.entity.SwipeEntity;
import com.miloo.modules.interaction.repository.MatchRepository;
import com.miloo.modules.interaction.repository.SwipeRepository;
import com.miloo.modules.people.dto.UserProfileDto;
import com.miloo.modules.people.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class InteractionServiceTest {

    private SwipeRepository swipeRepository;
    private MatchRepository matchRepository;
    private ProfileService profileService;
    private SimpMessagingTemplate messagingTemplate;
    private InteractionService interactionService;

    @BeforeEach
    void setUp() {
        swipeRepository = Mockito.mock(SwipeRepository.class);
        matchRepository = Mockito.mock(MatchRepository.class);
        profileService = Mockito.mock(ProfileService.class);
        messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);

        interactionService = new InteractionService(
                swipeRepository,
                matchRepository,
                profileService,
                messagingTemplate
        );
    }

    @Test
    @DisplayName("Swipe LIKE without reciprocal like should not form a match")
    void testSwipeWithoutReciprocal() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        when(swipeRepository.findBySwiperIdAndTargetId(userA, userB)).thenReturn(Optional.empty());
        when(swipeRepository.save(any())).thenAnswer(invocation -> {
            SwipeEntity e = invocation.getArgument(0);
            e.setSwipeId(UUID.randomUUID());
            return e;
        });
        when(swipeRepository.existsBySwiperIdAndTargetIdAndActionIn(userB, userA, List.of("LIKE", "SUPERLIKE")))
                .thenReturn(false);

        SwipeResponseDto response = interactionService.recordSwipe(
                userA,
                SwipeRequestDto.builder().targetUserId(userB).action("LIKE").build()
        );

        assertNotNull(response);
        assertFalse(response.getIsMatch());
        assertNull(response.getMatch());
    }

    @Test
    @DisplayName("Swipe LIKE with reciprocal like must trigger atomic match and return match summary")
    void testReciprocalSwipeCreatesMatch() {
        UUID userA = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID userB = UUID.fromString("00000000-0000-0000-0000-000000000002");

        when(swipeRepository.findBySwiperIdAndTargetId(userA, userB)).thenReturn(Optional.empty());
        when(swipeRepository.save(any())).thenAnswer(invocation -> {
            SwipeEntity e = invocation.getArgument(0);
            e.setSwipeId(UUID.randomUUID());
            return e;
        });
        when(swipeRepository.existsBySwiperIdAndTargetIdAndActionIn(userB, userA, List.of("LIKE", "SUPERLIKE")))
                .thenReturn(true);

        when(matchRepository.findActiveMatchBetween(userA, userB)).thenReturn(Optional.empty());
        when(matchRepository.save(any())).thenAnswer(invocation -> {
            MatchEntity m = invocation.getArgument(0);
            m.setMatchId(UUID.randomUUID());
            return m;
        });

        UserProfileDto partnerDto = UserProfileDto.builder()
                .userId(userB)
                .firstName("Yuki")
                .build();
        when(profileService.getProfileById(userB, userA)).thenReturn(partnerDto);

        UserProfileDto myDto = UserProfileDto.builder()
                .userId(userA)
                .firstName("Alex")
                .build();
        when(profileService.getProfileById(userA, userB)).thenReturn(myDto);

        SwipeResponseDto response = interactionService.recordSwipe(
                userA,
                SwipeRequestDto.builder().targetUserId(userB).action("LIKE").build()
        );

        assertNotNull(response);
        assertTrue(response.getIsMatch());
        assertNotNull(response.getMatch());
        assertEquals(userB, response.getMatch().getPartnerProfile().getUserId());
        assertEquals("Yuki", response.getMatch().getPartnerProfile().getFirstName());
    }
}
