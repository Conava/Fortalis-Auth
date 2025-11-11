package io.fortalis.fortalisauth.controller;

import io.fortalis.fortalisauth.dto.*;
import io.fortalis.fortalisauth.entity.Account;
import io.fortalis.fortalisauth.repo.AccountMfaRepository;
import io.fortalis.fortalisauth.service.*;
import io.fortalis.fortalisauth.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    private final LoginChallengeService challenges;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        log.debug("Registering new account for Email: {}, Display Name: {}", req.email(), req.displayName());
        Account account = accounts.register(req.email(), req.password(), req.displayName());
        var pair = tokens.issueTokens(account);
        return new AuthResponse(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds(), pair.displayName(), false);
    }

    /**
     * Keeps backward compatibility for:
     *  - no-MFA users (issues tokens),
     *  - MFA users providing inline TOTP,
     *  - otherwise returns a login ticket like `/auth/login/start`.
     */
    @PostMapping("/login")
    public Object login(HttpServletRequest httpReq, @Valid @RequestBody LoginRequest req) {
        String ip = clientIp(httpReq);
        String principalKey = req.emailOrUsername().toLowerCase();
        rateLimiter.checkAndConsume("ip:" + ip, 20, 60);
        rateLimiter.checkAndConsume("login:" + principalKey, 5, 900);

        log.debug("Login attempt (deprecated endpoint) for: {} from {}", req.emailOrUsername(), ip);
        Account account = accounts.findByEmailOrUsername(req.emailOrUsername())
                .orElseThrow(() -> ApiException.unauthorized("invalid_credentials", "Bad credentials"));
        if (account.getPasswordHash() == null || !accounts.matches(req.password(), account.getPasswordHash())) {
            throw ApiException.unauthorized("invalid_credentials", "Bad credentials");
        }

        boolean mfaEnabled = mfas.findByAccountId(account.getId())
                .map(m -> m.isEnabled() && "TOTP".equals(m.getType()))
                .orElse(false);

        if (!mfaEnabled) {
            // No MFA required; return tokens immediately.
            rateLimiter.clear("login:" + principalKey);
            var pair = tokens.issueTokens(account);
            return new AuthResponse(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds(), pair.displayName(), false);
        }

        // MFA enabled: if inline code provided, verify and issue tokens.
        String code = req.mfaCode();
        if (code != null && !code.isBlank()) {
            boolean ok = mfaService.verify(account.getId(), code);
            if (!ok) throw ApiException.unauthorized("mfa_invalid", "Invalid MFA code");
            rateLimiter.clear("login:" + principalKey);
            var pair = tokens.issueTokens(account);
            return new AuthResponse(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds(), pair.displayName(), true);
        }

        // Otherwise, start MFA challenge like `/auth/login/start`.
        var ticket = challenges.create(account, List.of("TOTP")); // future: add "WEBAUTHN", "RECOVERY_CODE"
        return new LoginStartResponse(ticket, List.of("TOTP"));
    }

    // Two-step login: start -> complete

    @PostMapping("/login/start")
    public ResponseEntity<?> loginStart(HttpServletRequest httpReq, @Valid @RequestBody LoginStartRequest req) {
        String ip = clientIp(httpReq);
        String principalKey = req.emailOrUsername().toLowerCase();
        rateLimiter.checkAndConsume("ip:" + ip, 20, 60);
        rateLimiter.checkAndConsume("login:" + principalKey, 5, 900);

        log.debug("Login(start) for: {} from {}", req.emailOrUsername(), ip);
        Account account = accounts.findByEmailOrUsername(req.emailOrUsername())
                .orElseThrow(() -> ApiException.unauthorized("invalid_credentials", "Bad credentials"));
        if (account.getPasswordHash() == null || !accounts.matches(req.password(), account.getPasswordHash())) {
            throw ApiException.unauthorized("invalid_credentials", "Bad credentials");
        }

        boolean mfaEnabled = mfas.findByAccountId(account.getId())
                .map(m -> m.isEnabled() && "TOTP".equals(m.getType()))
                .orElse(false);

        if (!mfaEnabled) {
            // No MFA required; return tokens immediately.
            rateLimiter.clear("login:" + principalKey);
            var pair = tokens.issueTokens(account);
            return ResponseEntity.ok(new AuthResponse(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds(), pair.displayName(), false));
        }

        // MFA required; issue short-lived login ticket and allowed factors.
        var ticket = challenges.create(account, List.of("TOTP")); // future: add "WEBAUTHN", "RECOVERY_CODE"
        return ResponseEntity.ok(new LoginStartResponse(ticket, List.of("TOTP")));
    }

    @PostMapping("/login/complete")
    public AuthResponse loginComplete(HttpServletRequest httpReq, @Valid @RequestBody LoginCompleteRequest req) {
        String ip = clientIp(httpReq);
        rateLimiter.checkAndConsume("mfa:" + req.loginTicket(), 10, 900);

        var challenge = challenges.consume(req.loginTicket())
                .orElseThrow(() -> ApiException.unauthorized("mfa_challenge_invalid", "Login ticket is invalid or expired"));

        // Validate selected factor.
        String factor = req.factor();
        if (!challenge.allowedFactors().contains(factor)) {
            throw ApiException.badRequest("mfa_factor_not_allowed", "Selected factor is not allowed for this login");
        }

        switch (factor) {
            case "TOTP" -> {
                String code = nonBlank(req.code(), "mfa_code_required", "TOTP code required");
                boolean ok = mfaService.verify(challenge.account().getId(), code);
                if (!ok) throw ApiException.unauthorized("mfa_invalid", "Invalid MFA code");
            }
            default -> throw ApiException.badRequest("mfa_factor_unsupported", "Unsupported MFA factor: " + factor);
        }

        log.debug("MFA complete from {}", ip);
        var pair = tokens.issueTokens(challenge.account());
        boolean mfaEnabled = true; // completing MFA implies enabled
        return new AuthResponse(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds(), pair.displayName(), mfaEnabled);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        log.debug("Token refresh attempt");
        var pair = tokens.refresh(req.refreshToken());

        boolean mfaEnabled = mfas.findByAccountId(pair.accountId())
                .map(m -> m.isEnabled() && "TOTP".equals(m.getType()))
                .orElse(false);

        return new AuthResponse(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds(), pair.displayName(), mfaEnabled);
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshRequest req) {
        log.debug("Logout attempt");
        tokens.revoke(req.refreshToken());
    }

    private static String clientIp(HttpServletRequest req) {
        String h = req.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) return h.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    private static String nonBlank(String v, @NotBlank String code, @NotBlank String msg) {
        if (v == null || v.isBlank()) throw ApiException.badRequest(code, msg);
        return v;
    }
}
