package io.fortalis.fortalisauth.it.support;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class TotpTestUtil {
    private static final int TIME_STEP_SECONDS = 30;

    private TotpTestUtil() {}

    public static String currentCodeFromBase32Secret(String base32Secret) {
        byte[] key = base32Decode(base32Secret);
        long counter = (System.currentTimeMillis() / 1000L) / TIME_STEP_SECONDS;
        int code = hotpSha1(key, counter);
        return String.format("%06d", code);
    }

    private static int hotpSha1(byte[] key, long counter) {
        try {
            byte[] msg = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] h = mac.doFinal(msg);

            int offset = h[h.length - 1] & 0x0F;
            int binCode = ((h[offset] & 0x7F) << 24)
                    | ((h[offset + 1] & 0xFF) << 16)
                    | ((h[offset + 2] & 0xFF) << 8)
                    | (h[offset + 3] & 0xFF);
            return binCode % 1_000_000;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] base32Decode(String s) {
        String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        int buffer = 0, bitsLeft = 0;
        byte[] out = new byte[s.length() * 5 / 8 + 8];
        int pos = 0;
        for (char c : s.toUpperCase().toCharArray()) {
            if (c == '=' || Character.isWhitespace(c)) continue;
            int val = alpha.indexOf(c);
            if (val < 0) throw new IllegalArgumentException("Invalid base32 char: " + c);
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out[pos++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        byte[] exact = new byte[pos];
        System.arraycopy(out, 0, exact, 0, pos);
        return exact;
    }
}