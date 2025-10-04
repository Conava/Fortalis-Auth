package io.fortalis.fortalisauth.repo;

import io.fortalis.fortalisauth.entity.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    long deleteByExpiresTsBefore(Instant time);
}
