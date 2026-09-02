package com.miloo.modules.people.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequestDto {

    @JsonProperty("first_name")
    private String firstName;

    private String birthdate;

    private String gender;

    @JsonProperty("interested_in")
    private String interestedIn;

    private String bio;

    private LocationDto location;

    @JsonProperty("max_distance_km")
    private Integer maxDistanceKm;

    @JsonProperty("age_min_pref")
    private Integer ageMinPref;

    @JsonProperty("age_max_pref")
    private Integer ageMaxPref;

    @JsonProperty("interest_tags")
    private List<String> interestTags;

    @JsonProperty("job_title")
    private String jobTitle;

    private String company;

    private String school;
}
