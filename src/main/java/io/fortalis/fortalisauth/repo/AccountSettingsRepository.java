package io.fortalis.fortalisauth.repo;

import io.fortalis.fortalisauth.entity.AccountSettings;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountSettingsRepository extends JpaRepository<AccountSettings, UUID> {
    Optional<AccountSettings> findByAccountId(UUID accountId);
}
