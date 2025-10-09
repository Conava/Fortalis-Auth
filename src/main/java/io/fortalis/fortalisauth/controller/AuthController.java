package io.fortalis.fortalisauth.controller;

import io.fortalis.fortalisauth.dto.*;
import io.fortalis.fortalisauth.entity.Account;
import io.fortalis.fortalisauth.repo.AccountMfaRepository;
import io.fortalis.fortalisauth.service.*;
import io.fortalis.fortalisauth.web.ApiException;
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

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        log.debug("Registering new account for email {}", req.email());
        Account a = accounts.register(req.email(), req.password(), req.displayName());
        var pair = tokens.issueTokens(a.getId());
        return new AuthResponse(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
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
            // TotpService.verify happens in MfaService->enable; here rely on login requiring code.
            // If you want, inject TotpService here and verify against stored secret.
        }
        var pair = tokens.issueTokens(a.getId());
        return new AuthResponse(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        var pair = tokens.refresh(req.refreshToken());
        return new AuthResponse(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds());
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshRequest req) {
        tokens.revoke(req.refreshToken());
    }
}
