package io.fortalis.fortalisauth.service;

import io.fortalis.fortalisauth.entity.PlayerServer;
import io.fortalis.fortalisauth.repo.PlayerServerRepository;
import io.fortalis.fortalisauth.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayerServerService {
    private final PlayerServerRepository playerRepository;

    public List<PlayerServer> getPlayersByAccount(UUID accountId) {
        return playerRepository.findByAccountId(accountId);
    }

    public Optional<PlayerServer> getPlayer(UUID accountId, String serverId) {
        return playerRepository.findByAccountIdAndServerId(accountId, serverId);
    }

    @Transactional
    public PlayerServer createPlayer(UUID accountId, String serverId, String characterName) {
        if (playerRepository.findByAccountIdAndServerId(accountId, serverId).isPresent()) {
            throw ApiException.badRequest("409", "Player already exists on server: " + serverId);
        }

        var player = new PlayerServer();
        player.setId(UUID.randomUUID());
        player.setAccountId(accountId);
        player.setServerId(serverId);
        return playerRepository.save(player);
    }

    @Transactional
    public void recordLogin(UUID accountId, String serverId) {
        playerRepository.findByAccountIdAndServerId(accountId, serverId)
            .ifPresent(player -> {
                player.setLastLoginTs(Instant.now());
                playerRepository.save(player);
            });
    }
}
