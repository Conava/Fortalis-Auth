package io.fortalis.fortalisauth.controller;

import io.fortalis.fortalisauth.crypto.TotpService;
import io.fortalis.fortalisauth.dto.MfaTotpSetupResponse;
import io.fortalis.fortalisauth.service.MfaService;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Minimal TOTP endpoints.
 * In a secured admin UI flow, you'd require an access token and read sub=accountId.
 */
@RestController
@RequestMapping("/auth/mfa/totp")
@RequiredArgsConstructor
@Slf4j
public class MfaController {
    private final MfaService mfaService;
    private final TotpService totp;

    /** For demo: pass accountId explicitly; in real app, use the access token subject. */
    @PostMapping("/setup")
    public MfaTotpSetupResponse setup(@RequestParam UUID accountId) {
        log.debug("Setup MFA with account {}", accountId);
        var result = mfaService.setupTotp(accountId);
        String url = totp.otpauthUrl("Fortalis", "acct:" + accountId, result.secretPlain());
        return new MfaTotpSetupResponse(result.secretPlain(), url, result.backupCodes());
    }

    @PostMapping("/enable")
    public void enable(@RequestParam UUID accountId, @RequestParam @NotBlank String code) {
        log.debug("Enable MFA with account {}", accountId);
        mfaService.enableTotp(accountId, code);
    }

    @PostMapping("/disable")
    public void disable(@RequestParam UUID accountId, @RequestParam @NotBlank String code) {
        log.debug("Disable MFA with account {}", accountId);
        mfaService.disableTotp(accountId, code);
    }
}
