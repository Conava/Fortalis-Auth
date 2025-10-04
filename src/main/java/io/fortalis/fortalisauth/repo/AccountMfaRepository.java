package io.fortalis.fortalisauth.repo;

import io.fortalis.fortalisauth.entity.AccountMfa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountMfaRepository extends JpaRepository<AccountMfa, UUID> {
    Optional<AccountMfa> findByAccountId(UUID accountId);
}
