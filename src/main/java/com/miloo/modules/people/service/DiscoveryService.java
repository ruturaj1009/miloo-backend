package com.miloo.modules.people.service;

import com.miloo.modules.people.dto.UserProfileDto;
import com.miloo.modules.people.entity.ProfileMediaEntity;
import com.miloo.modules.people.entity.UserProfileEntity;
import com.miloo.modules.people.repository.ProfileMediaRepository;
import com.miloo.modules.people.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscoveryService {

    private final UserProfileRepository profileRepository;
    private final ProfileMediaRepository mediaRepository;
    private final ProfileService profileService;

    @Transactional(readOnly = true)
    public List<UserProfileDto> getDiscoveryFeed(
            UUID currentUserId,
            Double customLat,
            Double customLon,
            Integer maxDistanceKm,
            Integer limit,
            Integer offset
    ) {
        int queryLimit = (limit != null && limit > 0) ? limit : 20;
        int queryOffset = (offset != null && offset >= 0) ? offset : 0;
        double radiusMeters = (maxDistanceKm != null && maxDistanceKm > 0 ? maxDistanceKm : 50) * 1000.0;

        double longitude = 139.6503; // Default Tokyo Shibuya
        double latitude = 35.6762;

        if (customLat != null && customLon != null) {
            latitude = customLat;
            longitude = customLon;
        } else {
            Optional<UserProfileEntity> myProfileOpt = profileRepository.findById(currentUserId);
            if (myProfileOpt.isPresent() && myProfileOpt.get().getLocation() != null) {
                longitude = myProfileOpt.get().getLocation().getX();
                latitude = myProfileOpt.get().getLocation().getY();
            }
        }

        List<UserProfileEntity> candidates = profileRepository.findDiscoveryCandidates(
                currentUserId,
                longitude,
                latitude,
                radiusMeters,
                queryLimit,
                queryOffset
        );

        // Fetch all media for these candidates in batch
        List<UUID> candidateIds = candidates.stream().map(UserProfileEntity::getUserId).toList();
        Map<UUID, List<ProfileMediaEntity>> mediaMap = candidateIds.isEmpty()
                ? Map.of()
                : mediaRepository.findAll().stream()
                .filter(m -> candidateIds.contains(m.getUserId()))
                .collect(Collectors.groupingBy(ProfileMediaEntity::getUserId));

        final double finalLon = longitude;
        final double finalLat = latitude;

        return candidates.stream().map(candidate -> {
            List<ProfileMediaEntity> photos = mediaMap.getOrDefault(candidate.getUserId(), List.of());
            Double distKm = profileRepository.calculateDistanceKm(currentUserId, candidate.getUserId());
            return profileService.toDto(candidate, photos, distKm != null ? distKm.intValue() : 0);
        }).collect(Collectors.toList());
    }
}
