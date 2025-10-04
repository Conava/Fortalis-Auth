package io.fortalis.fortalisauth.repo;

import io.fortalis.fortalisauth.entity.AccountIdentity;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountIdentityRepository extends JpaRepository<AccountIdentity, UUID> {
    Optional<AccountIdentity> findByProviderAndSubject(String provider, String subject);
}
