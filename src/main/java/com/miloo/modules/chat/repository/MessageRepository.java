package com.miloo.modules.chat.repository;

import com.miloo.modules.chat.entity.MessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

    List<MessageEntity> findByMatchIdOrderByCreatedAtAsc(UUID matchId);

    List<MessageEntity> findByMatchIdOrderByCreatedAtDesc(UUID matchId, Pageable pageable);

    Optional<MessageEntity> findTopByMatchIdOrderByCreatedAtDesc(UUID matchId);

    long countByMatchIdAndRecipientIdAndIsReadFalse(UUID matchId, UUID recipientId);

    @Modifying
    @Query("UPDATE MessageEntity m SET m.isRead = true WHERE m.matchId = :matchId AND m.recipientId = :recipientId AND m.isRead = false")
    int markMessagesAsRead(@Param("matchId") UUID matchId, @Param("recipientId") UUID recipientId);
}
