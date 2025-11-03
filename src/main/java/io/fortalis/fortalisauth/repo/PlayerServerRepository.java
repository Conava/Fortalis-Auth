package io.fortalis.fortalisauth.repo;

import io.fortalis.fortalisauth.entity.PlayerServer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerServerRepository extends JpaRepository<PlayerServer, UUID> {
    List<PlayerServer> findByAccountId(UUID accountId);
    Optional<PlayerServer> findByAccountIdAndServerId(UUID accountId, String serverId);
}
