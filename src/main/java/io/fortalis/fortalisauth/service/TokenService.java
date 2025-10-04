package io.fortalis.fortalisauth.service;

import io.fortalis.fortalisauth.config.AuthJwtProperties;
import io.fortalis.fortalisauth.crypto.JwtService;
import io.fortalis.fortalisauth.entity.*;
import io.fortalis.fortalisauth.repo.*;
import io.fortalis.fortalisauth.web.ApiException;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues access/refresh tokens and manages refresh store (hash+revocation).
 */
@Service
@RequiredArgsConstructor
public class TokenService {
    private final JwtService jwtService;
    private final AuthJwtProperties props;
    private final RefreshTokenRepository refreshTokens;
    private final AccountMfaRepository mfas;

    public record Pair(String accessToken, String refreshToken, long expiresInSeconds) {
    }

    @Transactional
    public Pair issueTokens(UUID accountId) {
        boolean mfa = mfas.findByAccountId(accountId).map(AccountMfa::isEnabled).orElse(false);
        String access = jwtService.createAccessToken(accountId, mfa);

        String refresh = randomToken();
        String refreshHash = hash(refresh);
        RefreshToken row = new RefreshToken();
        row.setAccountId(accountId);
        row.setTokenHash(refreshHash);
        row.setExpiresTs(Instant.now().plus(props.getRefreshTtl()));
        row.setRevoked(false);
        refreshTokens.save(row);

        long ttl = props.getAccessTtl().toSeconds();
        return new Pair(access, refresh, ttl);
    }

    @Transactional
    public Pair refresh(String refreshToken) {
        String hash = hash(refreshToken);
        RefreshToken row = refreshTokens.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> ApiException.unauthorized("invalid_refresh", "Invalid refresh token."));
        if (row.getExpiresTs().isBefore(Instant.now()))
            throw ApiException.unauthorized("expired_refresh", "Refresh token expired.");
        // Rotate
        row.setRevoked(true);
        refreshTokens.save(row);
        return issueTokens(row.getAccountId());
    }

    @Transactional
    public void revoke(String refreshToken) {
        refreshTokens.findByTokenHashAndRevokedFalse(hash(refreshToken)).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokens.save(rt);
        });
    }

    // --- helpers ---
    private static final SecureRandom RNG = new SecureRandom();

    private static String randomToken() {
        byte[] buf = new byte[32];
        RNG.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String hash(String token) {
        // lightweight SHA-256; Argon2 would be fine too.
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(h);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
