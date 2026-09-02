package com.miloo.modules.chat.repository;

import com.miloo.modules.chat.entity.CallSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CallSessionRepository extends JpaRepository<CallSessionEntity, UUID> {

    List<CallSessionEntity> findByMatchIdOrderByCreatedAtDesc(UUID matchId);
}
