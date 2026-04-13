package com.authservice.repo;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.authservice.model.OtpToken;
import com.authservice.model.OtpToken.Purpose;

public interface OtpRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findTopByEmailAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
            String email, Purpose purpose);

    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);
}
