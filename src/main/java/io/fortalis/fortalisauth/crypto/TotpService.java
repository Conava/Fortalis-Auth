package io.fortalis.fortalisauth.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

/**
 * Minimal TOTP (RFC 6238) verifier for 6-digit codes, 30s window.
 * Secret is Base32-encoded. Uses HMAC-SHA1 by default.
 */
@Service
public class TotpService {
    public boolean verify(String base32Secret, String code) {
        if (code == null || code.length() != 6) return false;
        long timestep = 30L;
        long t = Instant.now().getEpochSecond() / timestep;

        for (long offset = -1; offset <= 1; offset++) {
            String expected = generateCode(base32Secret, t + offset);
            if (expected.equals(code)) return true;
        }
        return false;
    }

    /**
     * For tests: generate the 6-digit code for a specific epoch second.
     */
    public String generateForTime(String base32Secret, long epochSecond) {
        long timestep = 30L;
        long t = epochSecond / timestep;
        return generateCode(base32Secret, t);
    }

    public String otpauthUrl(String issuer, String label, String base32Secret) {
        return "otpauth://totp/" + urlEncode(issuer) + ":" + urlEncode(label) +
                "?secret=" + base32Secret + "&issuer=" + urlEncode(issuer);
    }

    private static String generateCode(String base32Secret, long counter) {
        byte[] key = base32Decode(base32Secret);
        byte[] msg = ByteBuffer.allocate(8).putLong(counter).array();

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] h = mac.doFinal(msg);

            int offset = h[h.length - 1] & 0x0F;
            int bin = ((h[offset] & 0x7f) << 24) |
                    ((h[offset + 1] & 0xff) << 16) |
                    ((h[offset + 2] & 0xff) << 8) |
                    (h[offset + 3] & 0xff);

            int otp = bin % 1_000_000;
            return String.format("%06d", otp);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // Simple Base32 (RFC4648) decoder for A-Z2-7 without padding.
    private static byte[] base32Decode(String s) {
        String upper = s.replace("=", "").toUpperCase();
        String base32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        int buffer = 0, bitsLeft = 0;
        byte[] out = new byte[upper.length() * 5 / 8];
        int outIndex = 0;

        for (int i = 0; i < upper.length(); i++) {
            int val = base32.indexOf(upper.charAt(i));
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out[outIndex++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        if (outIndex == out.length) return out;
        byte[] trimmed = new byte[outIndex];
        System.arraycopy(out, 0, trimmed, 0, outIndex);
        return trimmed;
    }
}
