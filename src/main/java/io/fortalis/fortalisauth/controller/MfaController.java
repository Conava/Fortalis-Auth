package io.fortalis.fortalisauth.controller;

import io.fortalis.fortalisauth.config.AuthJwtProperties;
import io.fortalis.fortalisauth.crypto.TotpService;
import io.fortalis.fortalisauth.dto.MfaTotpSetupResponse;
import io.fortalis.fortalisauth.entity.AccountMfa;
import io.fortalis.fortalisauth.service.MfaService;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Minimal TOTP endpoints (using a dummy currentAccount for now).
 * In a secured admin UI flow, you'd require an access token and read sub=accountId.
 */
@RestController
@RequestMapping("/auth/mfa/totp")
@RequiredArgsConstructor
public class MfaController {
    private final MfaService mfaService;
    private final TotpService totp;
    private final AuthJwtProperties props;

    /** For demo: pass accountId explicitly; in real app, use the access token subject. */
    @PostMapping("/setup")
    public MfaTotpSetupResponse setup(@RequestParam UUID accountId) {
        AccountMfa mfa = mfaService.setupTotp(accountId);
        String url = totp.otpauthUrl("Fortalis", "acct:" + accountId, mfa.getSecret());
        return new MfaTotpSetupResponse(mfa.getSecret(), url);
    }

    @PostMapping("/enable")
    public void enable(@RequestParam UUID accountId, @RequestParam @NotBlank String code) {
        mfaService.enableTotp(accountId, code);
    }

    @PostMapping("/disable")
    public void disable(@RequestParam UUID accountId, @RequestParam @NotBlank String code) {
        mfaService.disableTotp(accountId, code);
    }
}
