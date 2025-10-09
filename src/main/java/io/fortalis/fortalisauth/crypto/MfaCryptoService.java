package io.fortalis.fortalisauth.crypto;

import io.fortalis.fortalisauth.config.CryptoProperties;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class MfaCryptoService {
    private static final String PREFIX = "enc:"; // enc:<kid>:<iv_b64url>:<ct_b64url>
    private static final int GCM_TAG_BITS = 128;

    private final String kid;
    private final SecretKey key;
    private final SecureRandom rng = new SecureRandom();
    private final boolean passthrough;

    public MfaCryptoService(CryptoProperties props) {
        String configuredKid = props.mfaKeyId();
        String b64 = props.mfaEncryptionKey();
        if (b64 == null || b64.isBlank()) {
            this.passthrough = true;
            this.kid = Objects.requireNonNullElse(configuredKid, "dev");
            this.key = null;
        } else {
            byte[] raw = Base64.getDecoder().decode(b64);
            if (raw.length != 32) throw new IllegalArgumentException("MFA key must be 32 bytes (AES-256)");
            this.passthrough = false;
            this.kid = Objects.requireNonNullElse(configuredKid, "v1");
            this.key = new SecretKeySpec(raw, "AES");
        }
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        if (passthrough) return plaintext;
        try {
            byte[] iv = new byte[12];
            rng.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return PREFIX + kid + ':' + b64u(iv) + ':' + b64u(ct);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encrypt failed", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null) return null;
        if (!stored.startsWith(PREFIX)) return stored; // plaintext fallback
        if (passthrough) return stored; // cannot decrypt without key; leave as-is
        String[] parts = stored.split(":", 4);
        if (parts.length != 4) throw new IllegalArgumentException("Invalid envelope");
        String ivB64 = parts[2];
        String ctB64 = parts[3];
        try {
            byte[] iv = b64uDec(ivB64);
            byte[] ct = b64uDec(ctB64);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Decrypt failed", e);
        }
    }

    public boolean isEncrypted(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    private static String b64u(byte[] b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static byte[] b64uDec(String s) {
        return Base64.getUrlDecoder().decode(s);
    }
}

