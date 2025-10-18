package io.fortalis.fortalisauth.controller;

import io.fortalis.fortalisauth.crypto.TotpService;
import io.fortalis.fortalisauth.dto.MfaTotpSetupResponse;
import io.fortalis.fortalisauth.service.MfaService;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * TOTP endpoints that derive accountId from the authenticated JWT subject.
 */
@RestController
@RequestMapping("/auth/mfa/totp")
@RequiredArgsConstructor
@Slf4j
public class MfaController {
    private final MfaService mfaService;
    private final TotpService totp;

    @PostMapping("/setup")
    public MfaTotpSetupResponse setup(@AuthenticationPrincipal Jwt jwt) {
        UUID accountId = UUID.fromString(jwt.getSubject());
        log.debug("Setup MFA with account {}", accountId);
        var result = mfaService.setupTotp(accountId);
        String url = totp.otpauthUrl("Fortalis", "acct:" + accountId, result.secretPlain());
        return new MfaTotpSetupResponse(result.secretPlain(), url, result.backupCodes());
    }

    @PostMapping("/enable")
    public void enable(@AuthenticationPrincipal Jwt jwt, @RequestParam @NotBlank String code) {
        UUID accountId = UUID.fromString(jwt.getSubject());
        log.debug("Enable MFA with account {}", accountId);
        mfaService.enableTotp(accountId, code);
    }

    @PostMapping("/disable")
    public void disable(@AuthenticationPrincipal Jwt jwt, @RequestParam @NotBlank String code) {
        UUID accountId = UUID.fromString(jwt.getSubject());
        log.debug("Disable MFA with account {}", accountId);
        mfaService.disableTotp(accountId, code);
    }
}

