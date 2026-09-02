package com.miloo.modules.people.repository;

import com.miloo.modules.people.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {

    @Query(value = """
            SELECT p.*
            FROM people_ctx.user_profiles p
            WHERE p.user_id != :currentUserId
              AND p.is_active = TRUE
              AND p.user_id NOT IN (
                  SELECT s.target_id FROM interaction_ctx.swipes s WHERE s.swiper_id = :currentUserId
              )
              AND ST_DWithin(p.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :maxDistanceMeters)
            ORDER BY ST_Distance(p.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) ASC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<UserProfileEntity> findDiscoveryCandidates(
            @Param("currentUserId") UUID currentUserId,
            @Param("longitude") double longitude,
            @Param("latitude") double latitude,
            @Param("maxDistanceMeters") double maxDistanceMeters,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
            SELECT ST_Distance(
                (SELECT location FROM people_ctx.user_profiles WHERE user_id = :userA),
                (SELECT location FROM people_ctx.user_profiles WHERE user_id = :userB)
            ) / 1000.0
            """, nativeQuery = true)
    Double calculateDistanceKm(@Param("userA") UUID userA, @Param("userB") UUID userB);
}
