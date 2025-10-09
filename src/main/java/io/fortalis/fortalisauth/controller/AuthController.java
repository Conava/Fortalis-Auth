package io.fortalis.fortalisauth.controller;

import io.fortalis.fortalisauth.dto.*;
import io.fortalis.fortalisauth.entity.Account;
import io.fortalis.fortalisauth.repo.AccountMfaRepository;
import io.fortalis.fortalisauth.service.*;
import io.fortalis.fortalisauth.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Registration, login, refresh, logout.
 * The access token (JWT) sub=account_id is used by region servers.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AccountService accounts;
    private final TokenService tokens;
    private final AccountMfaRepository mfas;
    private final RateLimiterService rateLimiter;
    private final MfaService mfaService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        log.debug("Registering new account for Email: {}, Display Name: {}", req.email(), req.displayName());
        Account account = accounts.register(req.email(), req.password(), req.displayName());
        var pair = tokens.issueTokens(account.getId());
        return new AuthResponse(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds());
    }

    @PostMapping("/login")
    public AuthResponse login(HttpServletRequest httpReq, @Valid @RequestBody LoginRequest req) {
        String ip = clientIp(httpReq);
        String principalKey = req.emailOrUsername().toLowerCase();
        rateLimiter.checkAndConsume("ip:" + ip, 20, 60);
        rateLimiter.checkAndConsume("login:" + principalKey, 5, 900);

        log.debug("Login attempt for: {} from {}", req.emailOrUsername(), ip);
        Account a = accounts.findByEmailOrUsername(req.emailOrUsername())
                .orElseThrow(() -> ApiException.unauthorized("invalid_credentials", "Bad credentials"));
        if (a.getPasswordHash() == null || !accounts.matches(req.password(), a.getPasswordHash())) {
            throw ApiException.unauthorized("invalid_credentials", "Bad credentials");
        }
        boolean mfaEnabled = mfas.findByAccountId(a.getId()).map(m -> m.isEnabled() && "TOTP".equals(m.getType())).orElse(false);
        if (mfaEnabled) {
            String code = req.mfaCode();
            if (code == null || code.isBlank())
                throw ApiException.unauthorized("mfa_required", "TOTP code required");
            boolean ok = mfaService.verify(a.getId(), code);
            if (!ok) {
                throw ApiException.unauthorized("mfa_invalid", "Invalid MFA code");
            }
        }
        rateLimiter.clear("login:" + principalKey);
        var pair = tokens.issueTokens(a.getId());
        return new AuthResponse(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        log.debug("Token refresh attempt for refresh token: {}", req.refreshToken());
        var pair = tokens.refresh(req.refreshToken());
        return new AuthResponse(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds());
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshRequest req) {
        log.debug("Logout attempt for refresh token: {}", req.refreshToken());
        tokens.revoke(req.refreshToken());
    }

    private static String clientIp(HttpServletRequest req) {
        String h = req.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) return h.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
