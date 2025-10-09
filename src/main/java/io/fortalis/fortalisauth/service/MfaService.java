package io.fortalis.fortalisauth.service;

import io.fortalis.fortalisauth.crypto.MfaCryptoService;
import io.fortalis.fortalisauth.crypto.TotpService;
import io.fortalis.fortalisauth.entity.AccountMfa;
import io.fortalis.fortalisauth.entity.MfaBackupCode;
import io.fortalis.fortalisauth.repo.AccountMfaRepository;
import io.fortalis.fortalisauth.repo.MfaBackupCodeRepository;
import io.fortalis.fortalisauth.web.ApiException;

import java.security.SecureRandom;
import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles TOTP lifecycle: setup (secret), enable, disable, verify during login.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {
    private final AccountMfaRepository repo;
    private final MfaBackupCodeRepository backupRepo;
    private final TotpService totp;
    private final MfaCryptoService crypto;

    public record SetupResult(String secretPlain, List<String> backupCodes, AccountMfa mfaRow) {
    }

    public static String randomBase32() {
        byte[] buf = new byte[20];
        new SecureRandom().nextBytes(buf);
        return base32(buf);
    }

    @Transactional
    public SetupResult setupTotp(UUID accountId) {
        String secretPlain = randomBase32();
        AccountMfa mfa = repo.findByAccountId(accountId).orElseGet(() -> {
            var m = new AccountMfa();
            m.setAccountId(accountId);
            m.setType("TOTP");
            m.setEnabled(false);
            return m;
        });
        mfa.setSecret(crypto.encrypt(secretPlain));
        mfa.setType("TOTP");
        mfa.setEnabled(false);
        mfa = repo.save(mfa);

        backupRepo.deleteByAccountId(accountId);
        List<String> codes = generateBackupCodes(10);
        for (String code : codes) {
            MfaBackupCode row = new MfaBackupCode();
            row.setAccountId(accountId);
            row.setCodeHash(sha256Base64(code));
            row.setUsed(false);
            backupRepo.save(row);
        }
        return new SetupResult(secretPlain, codes, mfa);
    }

    @Transactional
    public void enableTotp(UUID accountId, String code) {
        AccountMfa mfa = repo.findByAccountId(accountId)
                .orElseThrow(() -> ApiException.badRequest("totp_not_setup", "Call setup first."));
        if (!"TOTP".equals(mfa.getType())) throw ApiException.badRequest("totp_wrong_type", "Wrong type");
        String secret = crypto.decrypt(mfa.getSecret());
        if (!totp.verify(secret, code)) throw ApiException.badRequest("totp_invalid", "Invalid code");
        mfa.setEnabled(true);
        repo.save(mfa);
        log.info("MFA enabled for account {}", accountId);
    }

    @Transactional
    public void disableTotp(UUID accountId, String code) {
        AccountMfa mfa = repo.findByAccountId(accountId)
                .orElseThrow(() -> ApiException.badRequest("totp_not_setup", "No TOTP on account"));
        String secret = crypto.decrypt(mfa.getSecret());
        if (!totp.verify(secret, code)) throw ApiException.badRequest("totp_invalid", "Invalid code");
        mfa.setEnabled(false);
        repo.save(mfa);
        log.info("MFA disabled for account {}", accountId);
    }

    /**
     * Verify a provided code: either a valid 6-digit TOTP, or a matching unused backup code.
     * If a backup code matches, it's marked as used.
     */
    @Transactional
    public boolean verify(UUID accountId, String code) {
        String trimmed = code == null ? null : code.trim();
        if (trimmed == null || trimmed.isEmpty()) return false;
        Optional<AccountMfa> opt = repo.findByAccountId(accountId);
        if (opt.isEmpty() || !opt.get().isEnabled()) return false;
        AccountMfa mfa = opt.get();
        if ("TOTP".equals(mfa.getType()) && trimmed.matches("\\d{6}")) {
            String secret = crypto.decrypt(mfa.getSecret());
            boolean ok = totp.verify(secret, trimmed);
            if (ok) return true;
        }
        // try backup code
        String hash = sha256Base64(trimmed);
        return backupRepo.findFirstByAccountIdAndCodeHashAndUsedFalse(accountId, hash)
                .map(row -> {
                    row.setUsed(true);
                    backupRepo.save(row);
                    log.info("MFA backup code consumed for account {}", accountId);
                    return true;
                }).orElse(false);
    }

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

    private static List<String> generateBackupCodes(int count) {
        SecureRandom rng = new SecureRandom();
        List<String> codes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            // 10-digit numeric code groups like 1234-5678
            int part1 = rng.nextInt(10000);
            int part2 = rng.nextInt(10000);
            codes.add(String.format("%04d-%04d", part1, part2));
        }
        return codes;
    }

    private static String sha256Base64(String s) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(h);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
