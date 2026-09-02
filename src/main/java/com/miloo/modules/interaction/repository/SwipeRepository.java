package com.miloo.modules.interaction.repository;

import com.miloo.modules.interaction.entity.SwipeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SwipeRepository extends JpaRepository<SwipeEntity, UUID> {

    Optional<SwipeEntity> findBySwiperIdAndTargetId(UUID swiperId, UUID targetId);

    boolean existsBySwiperIdAndTargetIdAndActionIn(UUID swiperId, UUID targetId, Collection<String> actions);
}
