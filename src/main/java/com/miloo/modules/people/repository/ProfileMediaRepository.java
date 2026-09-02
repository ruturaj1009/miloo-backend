package com.miloo.modules.people.repository;

import com.miloo.modules.people.entity.ProfileMediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProfileMediaRepository extends JpaRepository<ProfileMediaEntity, UUID> {

    List<ProfileMediaEntity> findByUserIdOrderByDisplayOrderAsc(UUID userId);

    void deleteByMediaIdAndUserId(UUID mediaId, UUID userId);
}
