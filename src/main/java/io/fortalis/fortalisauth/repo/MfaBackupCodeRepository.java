package io.fortalis.fortalisauth.repo;

import io.fortalis.fortalisauth.entity.MfaBackupCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MfaBackupCodeRepository extends JpaRepository<MfaBackupCode, UUID> {
    Optional<MfaBackupCode> findFirstByAccountIdAndCodeHashAndUsedFalse(UUID accountId, String codeHash);
    void deleteByAccountId(UUID accountId);
}

