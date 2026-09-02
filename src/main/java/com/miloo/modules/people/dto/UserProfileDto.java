package com.miloo.modules.people.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("birthdate")
    private String birthdate;

    private Integer age;

    private String gender;

    @JsonProperty("interested_in")
    private String interestedIn;

    private String bio;

    private LocationDto location;

    @JsonProperty("distance_km")
    private Integer distanceKm;

    @JsonProperty("max_distance_km")
    private Integer maxDistanceKm;

    @JsonProperty("age_min_pref")
    private Integer ageMinPref;

    @JsonProperty("age_max_pref")
    private Integer ageMaxPref;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("is_verified")
    private Boolean isVerified;

    @JsonProperty("interest_tags")
    private List<String> interestTags;

    private List<ProfileMediaDto> photos;

    @JsonProperty("job_title")
    private String jobTitle;

    private String company;

    private String school;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;
}
