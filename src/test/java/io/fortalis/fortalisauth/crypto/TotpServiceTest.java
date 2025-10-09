package io.fortalis.fortalisauth.crypto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TotpServiceTest {
    private static final String RFC_SECRET_BASE32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"; // "12345678901234567890"

    @Test
    void generateForTime_matchesRfcVectorsAtCounter0and1_with6Digits() {
        TotpService totp = new TotpService();
        // epoch=0 -> counter=0 -> HOTP 6-digit should be 755224
        assertEquals("755224", totp.generateForTime(RFC_SECRET_BASE32, 0));
        // epoch=59 -> counter=1 -> HOTP 6-digit 
        assertEquals("287082", totp.generateForTime(RFC_SECRET_BASE32, 59));
    }

    @Test
    void verify_rejectsInvalidCodeLengthOrDigits() {
        TotpService totp = new TotpService();
        assertFalse(totp.verify(RFC_SECRET_BASE32, null));
        assertFalse(totp.verify(RFC_SECRET_BASE32, ""));
        assertFalse(totp.verify(RFC_SECRET_BASE32, "12345"));
        assertFalse(totp.verify(RFC_SECRET_BASE32, "1234567"));
        assertFalse(totp.verify(RFC_SECRET_BASE32, "abcdef"));
    }
}

