package com.miloo.modules.interaction.repository;

import com.miloo.modules.interaction.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<MatchEntity, UUID> {

    @Query("""
            SELECT m FROM MatchEntity m
            WHERE (m.userOneId = :userId OR m.userTwoId = :userId)
              AND m.isActive = true
            ORDER BY m.createdAt DESC
            """)
    List<MatchEntity> findActiveMatchesByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT m FROM MatchEntity m
            WHERE ((m.userOneId = :u1 AND m.userTwoId = :u2) OR (m.userOneId = :u2 AND m.userTwoId = :u1))
              AND m.isActive = true
            """)
    Optional<MatchEntity> findActiveMatchBetween(@Param("u1") UUID u1, @Param("u2") UUID u2);
}
