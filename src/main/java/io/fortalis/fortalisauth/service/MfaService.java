package io.fortalis.fortalisauth.service;

import io.fortalis.fortalisauth.crypto.TotpService;
import io.fortalis.fortalisauth.entity.AccountMfa;
import io.fortalis.fortalisauth.repo.AccountMfaRepository;
import io.fortalis.fortalisauth.web.ApiException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles TOTP lifecycle: setup (secret), enable, disable, verify during login.
 */
@Service
@RequiredArgsConstructor
public class MfaService {
    private final AccountMfaRepository repo;
    private final TotpService totp;

    public static String randomBase32() {
        byte[] buf = new byte[20];
        new SecureRandom().nextBytes(buf);
        return base32(buf);
    }

    @Transactional
    public AccountMfa setupTotp(UUID accountId) {
        AccountMfa mfa = repo.findByAccountId(accountId).orElseGet(() -> {
            var m = new AccountMfa();
            m.setAccountId(accountId);
            m.setType("TOTP");
            m.setEnabled(false);
            return m;
        });
        mfa.setSecret(randomBase32());
        mfa.setType("TOTP");
        mfa.setEnabled(false);
        return repo.save(mfa);
    }

    @Transactional
    public void enableTotp(UUID accountId, String code) {
        AccountMfa mfa = repo.findByAccountId(accountId)
                .orElseThrow(() -> ApiException.badRequest("totp_not_setup", "Call setup first."));
        if (!"TOTP".equals(mfa.getType())) throw ApiException.badRequest("totp_wrong_type", "Wrong type");
        if (!totp.verify(mfa.getSecret(), code)) throw ApiException.badRequest("totp_invalid", "Invalid code");
        mfa.setEnabled(true);
        repo.save(mfa);
    }

    @Transactional
    public void disableTotp(UUID accountId, String code) {
        AccountMfa mfa = repo.findByAccountId(accountId)
                .orElseThrow(() -> ApiException.badRequest("totp_not_setup", "No TOTP on account"));
        if (!totp.verify(mfa.getSecret(), code)) throw ApiException.badRequest("totp_invalid", "Invalid code");
        mfa.setEnabled(false);
        repo.save(mfa);
    }

    // --- base32 helper (RFC4648, no padding)
    private static String base32(byte[] bytes) {
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder out = new StringBuilder((bytes.length * 8 + 4) / 5);
        int buffer = 0, bitsLeft = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int idx = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                out.append(alphabet.charAt(idx));
            }
        }
        if (bitsLeft > 0) {
            int idx = (buffer << (5 - bitsLeft)) & 0x1F;
            out.append(alphabet.charAt(idx));
        }
        return out.toString();
    }
}
