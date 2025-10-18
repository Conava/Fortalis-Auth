package io.fortalis.fortalisauth.service;

import io.fortalis.fortalisauth.entity.Account;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * In-memory short-lived login challenge storage.
 * Stores the authenticated Account until MFA completion.
 */
@Service
public class LoginChallengeService {

    public record Challenge(Account account, Instant expiresAt, List<String> allowedFactors) {}

    private static final long TTL_SECONDS = 300; // 5 minutes
    private final Map<String, Challenge> store = new ConcurrentHashMap<>();

    public String create(Account account, List<String> allowedFactors) {
        var ticket = UUID.randomUUID().toString();
        var challenge = new Challenge(account, Instant.now().plusSeconds(TTL_SECONDS), List.copyOf(allowedFactors));
        store.put(ticket, challenge);
        return ticket;
    }

    public Optional<Challenge> peek(String ticket) {
        var ch = store.get(ticket);
        if (ch == null) return Optional.empty();
        if (Instant.now().isAfter(ch.expiresAt())) {
            store.remove(ticket);
            return Optional.empty();
        }
        return Optional.of(ch);
    }

    public Optional<Challenge> consume(String ticket) {
        var opt = peek(ticket);
        opt.ifPresent(c -> store.remove(ticket));
        return opt;
    }

    public void clearExpired() {
        var now = Instant.now();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }
}
