package com.miloo.modules.people.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_profiles", schema = "people_ctx")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileEntity {

    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "birthdate", nullable = false)
    private LocalDate birthdate;

    @Column(name = "gender", nullable = false, length = 20)
    private String gender;

    @Column(name = "interested_in", nullable = false, length = 20)
    private String interestedIn;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "location", columnDefinition = "geography(Point,4326)", nullable = false)
    private Point location;

    @Builder.Default
    @Column(name = "max_distance_km")
    private Integer maxDistanceKm = 50;

    @Builder.Default
    @Column(name = "age_min_pref")
    private Integer ageMinPref = 18;

    @Builder.Default
    @Column(name = "age_max_pref")
    private Integer ageMaxPref = 35;

    @Column(name = "interest_tags", columnDefinition = "TEXT[]")
    private String[] interestTags;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(name = "company", length = 100)
    private String company;

    @Column(name = "school", length = 100)
    private String school;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
