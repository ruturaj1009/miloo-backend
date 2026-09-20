package com.miloo.modules.auth.repository;

import com.miloo.common.otp.OtpStatus;
import com.miloo.modules.auth.entity.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<OtpEntity, UUID> {

    /**
     * Finds the most recent OTP entry for a given destination and status (e.g. PENDING).
     */
    Optional<OtpEntity> findTopByDestinationAndStatusOrderByCreatedAtDesc(String destination, OtpStatus status);

    /**
     * Invalidate/supersede all existing pending OTPs for a given destination.
     */
    @Modifying
    @Query("UPDATE OtpEntity o SET o.status = :newStatus WHERE o.destination = :destination AND o.status = :currentStatus")
    void updateStatusByDestinationAndStatus(
            @Param("destination") String destination,
            @Param("currentStatus") OtpStatus currentStatus,
            @Param("newStatus") OtpStatus newStatus
    );
}
