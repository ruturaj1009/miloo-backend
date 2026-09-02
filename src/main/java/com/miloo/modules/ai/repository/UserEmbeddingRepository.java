package com.miloo.modules.ai.repository;

import com.miloo.modules.ai.entity.UserEmbeddingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserEmbeddingRepository extends JpaRepository<UserEmbeddingEntity, UUID> {
}
