package com.miloo.modules.people.service;

import com.miloo.common.exception.ApiException;
import com.miloo.modules.people.dto.*;
import com.miloo.modules.people.entity.ProfileMediaEntity;
import com.miloo.modules.people.entity.UserProfileEntity;
import com.miloo.modules.people.repository.ProfileMediaRepository;
import com.miloo.modules.people.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserProfileRepository profileRepository;
    private final ProfileMediaRepository mediaRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional(readOnly = true)
    public UserProfileDto getMyProfile(UUID userId) {
        UserProfileEntity entity = profileRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found for current user"));
        List<ProfileMediaEntity> photos = mediaRepository.findByUserIdOrderByDisplayOrderAsc(userId);
        return toDto(entity, photos, null);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfileById(UUID targetUserId, UUID currentUserId) {
        UserProfileEntity entity = profileRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Target profile not found"));
        List<ProfileMediaEntity> photos = mediaRepository.findByUserIdOrderByDisplayOrderAsc(targetUserId);
        Double distance = profileRepository.calculateDistanceKm(currentUserId, targetUserId);
        return toDto(entity, photos, distance != null ? distance.intValue() : null);
    }

    @Transactional
    public UserProfileDto updateMyProfile(UUID userId, UpdateProfileRequestDto req) {
        UserProfileEntity entity = profileRepository.findById(userId)
                .orElseGet(() -> UserProfileEntity.builder().userId(userId).build());

        if (req.getFirstName() != null) entity.setFirstName(req.getFirstName());
        if (req.getBirthdate() != null) entity.setBirthdate(LocalDate.parse(req.getBirthdate()));
        if (req.getGender() != null) entity.setGender(req.getGender());
        if (req.getInterestedIn() != null) entity.setInterestedIn(req.getInterestedIn());
        if (req.getBio() != null) entity.setBio(req.getBio());

        if (req.getLocation() != null && req.getLocation().getLatitude() != null && req.getLocation().getLongitude() != null) {
            Point point = geometryFactory.createPoint(new Coordinate(
                    req.getLocation().getLongitude(),
                    req.getLocation().getLatitude()
            ));
            point.setSRID(4326);
            entity.setLocation(point);
        } else if (entity.getLocation() == null) {
            // Default location: Tokyo Shibuya if unset
            Point defaultPoint = geometryFactory.createPoint(new Coordinate(139.7016, 35.6580));
            defaultPoint.setSRID(4326);
            entity.setLocation(defaultPoint);
        }

        if (req.getMaxDistanceKm() != null) entity.setMaxDistanceKm(req.getMaxDistanceKm());
        if (req.getAgeMinPref() != null) entity.setAgeMinPref(req.getAgeMinPref());
        if (req.getAgeMaxPref() != null) entity.setAgeMaxPref(req.getAgeMaxPref());
        if (req.getInterestTags() != null) entity.setInterestTags(req.getInterestTags().toArray(new String[0]));
        if (req.getJobTitle() != null) entity.setJobTitle(req.getJobTitle());
        if (req.getCompany() != null) entity.setCompany(req.getCompany());
        if (req.getSchool() != null) entity.setSchool(req.getSchool());

        if (entity.getFirstName() == null) entity.setFirstName("New User");
        if (entity.getBirthdate() == null) entity.setBirthdate(LocalDate.of(2000, 1, 1));
        if (entity.getGender() == null) entity.setGender("OTHER");
        if (entity.getInterestedIn() == null) entity.setInterestedIn("EVERYONE");

        UserProfileEntity saved = profileRepository.save(entity);
        List<ProfileMediaEntity> photos = mediaRepository.findByUserIdOrderByDisplayOrderAsc(userId);
        return toDto(saved, photos, 0);
    }

    @Transactional
    public ProfileMediaDto addPhoto(UUID userId, ProfileMediaDto mediaDto) {
        ProfileMediaEntity media = ProfileMediaEntity.builder()
                .userId(userId)
                .r2ObjectKey(mediaDto.getR2ObjectKey())
                .mediaUrl(mediaDto.getMediaUrl())
                .displayOrder(mediaDto.getDisplayOrder() != null ? mediaDto.getDisplayOrder() : 0)
                .build();

        ProfileMediaEntity saved = mediaRepository.save(media);
        return toMediaDto(saved);
    }

    @Transactional
    public void deletePhoto(UUID userId, UUID mediaId) {
        mediaRepository.deleteByMediaIdAndUserId(mediaId, userId);
    }

    public UserProfileDto toDto(UserProfileEntity entity, List<ProfileMediaEntity> photos, Integer distanceKm) {
        int age = 25;
        if (entity.getBirthdate() != null) {
            age = Period.between(entity.getBirthdate(), LocalDate.now()).getYears();
        }

        LocationDto locDto = null;
        if (entity.getLocation() != null) {
            locDto = LocationDto.builder()
                    .latitude(entity.getLocation().getY())
                    .longitude(entity.getLocation().getX())
                    .build();
        }

        List<ProfileMediaDto> photoDtos = photos != null
                ? photos.stream().map(this::toMediaDto).collect(Collectors.toList())
                : List.of();

        List<String> tags = entity.getInterestTags() != null
                ? Arrays.asList(entity.getInterestTags())
                : List.of();

        return UserProfileDto.builder()
                .userId(entity.getUserId())
                .firstName(entity.getFirstName())
                .birthdate(entity.getBirthdate() != null ? entity.getBirthdate().toString() : null)
                .age(age)
                .gender(entity.getGender())
                .interestedIn(entity.getInterestedIn())
                .bio(entity.getBio())
                .location(locDto)
                .distanceKm(distanceKm != null ? distanceKm : 0)
                .maxDistanceKm(entity.getMaxDistanceKm())
                .ageMinPref(entity.getAgeMinPref())
                .ageMaxPref(entity.getAgeMaxPref())
                .isActive(entity.getIsActive())
                .isVerified(true)
                .interestTags(tags)
                .photos(photoDtos)
                .jobTitle(entity.getJobTitle())
                .company(entity.getCompany())
                .school(entity.getSchool())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null)
                .build();
    }

    public ProfileMediaDto toMediaDto(ProfileMediaEntity entity) {
        return ProfileMediaDto.builder()
                .mediaId(entity.getMediaId())
                .userId(entity.getUserId())
                .r2ObjectKey(entity.getR2ObjectKey())
                .mediaUrl(entity.getMediaUrl())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .build();
    }
}
