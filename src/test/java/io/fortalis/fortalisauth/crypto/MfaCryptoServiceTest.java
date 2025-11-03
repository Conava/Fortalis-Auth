package io.fortalis.fortalisauth.crypto;

import io.fortalis.fortalisauth.config.CryptoProperties;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class MfaCryptoServiceTest {

    @Test
    void passthroughMode_whenNoKeyConfigured_returnsPlaintext() {
        var props = new CryptoProperties("dev", null);
        var service = new MfaCryptoService(props);

        var plaintext = "my-secret-value";
        var encrypted = service.encrypt(plaintext);
        var decrypted = service.decrypt(encrypted);

        assertEquals(plaintext, encrypted, "Should return plaintext in passthrough mode");
        assertEquals(plaintext, decrypted, "Should decrypt to same plaintext");
        assertFalse(service.isEncrypted(plaintext), "Plaintext should not be marked as encrypted");
    }

    @Test
    void passthroughMode_whenBlankKey_returnsPlaintext() {
        var props = new CryptoProperties("dev", "");
        var service = new MfaCryptoService(props);

        var plaintext = "test-secret";
        assertEquals(plaintext, service.encrypt(plaintext));
    }

    @Test
    void encrypt_whenKeyProvided_returnsEncryptedEnvelope() {
        var key32Bytes = Base64.getEncoder().encodeToString(new byte[32]); // 32-byte zero key
        var props = new CryptoProperties("v1", key32Bytes);
        var service = new MfaCryptoService(props);

        var plaintext = "sensitive-data";
        var encrypted = service.encrypt(plaintext);

        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("enc:"), "Should start with enc: prefix");
        assertTrue(encrypted.contains("v1"), "Should contain key ID");
        assertTrue(service.isEncrypted(encrypted), "Should be marked as encrypted");
        assertNotEquals(plaintext, encrypted, "Should not equal plaintext");
    }

    @Test
    void decrypt_whenKeyProvided_returnsOriginalPlaintext() {
        var key32Bytes = Base64.getEncoder().encodeToString(new byte[32]);
        var props = new CryptoProperties("v1", key32Bytes);
        var service = new MfaCryptoService(props);

        var plaintext = "my-totp-secret";
        var encrypted = service.encrypt(plaintext);
        var decrypted = service.decrypt(encrypted);

        assertEquals(plaintext, decrypted, "Should decrypt to original plaintext");
    }

    @Test
    void decrypt_withDifferentValues_producesCorrectResults() {
        var key32Bytes = Base64.getEncoder().encodeToString(new byte[32]);
        var props = new CryptoProperties("test", key32Bytes);
        var service = new MfaCryptoService(props);

        var plaintext1 = "first-secret";
        var plaintext2 = "second-secret";

        var encrypted1 = service.encrypt(plaintext1);
        var encrypted2 = service.encrypt(plaintext2);

        assertNotEquals(encrypted1, encrypted2, "Different plaintexts should produce different ciphertexts");
        assertEquals(plaintext1, service.decrypt(encrypted1));
        assertEquals(plaintext2, service.decrypt(encrypted2));
    }

    @Test
    void encrypt_withSamePlaintext_producesUniqueIV() {
        var key32Bytes = Base64.getEncoder().encodeToString(new byte[32]);
        var props = new CryptoProperties("v1", key32Bytes);
        var service = new MfaCryptoService(props);

        var plaintext = "same-value";
        var encrypted1 = service.encrypt(plaintext);
        var encrypted2 = service.encrypt(plaintext);

        assertNotEquals(encrypted1, encrypted2, "Same plaintext should produce different ciphertexts due to random IV");
        assertEquals(plaintext, service.decrypt(encrypted1));
        assertEquals(plaintext, service.decrypt(encrypted2));
    }

    @Test
    void encrypt_withNull_returnsNull() {
        var key32Bytes = Base64.getEncoder().encodeToString(new byte[32]);
        var props = new CryptoProperties("v1", key32Bytes);
        var service = new MfaCryptoService(props);

        assertNull(service.encrypt(null));
    }

    @Test
    void decrypt_withNull_returnsNull() {
        var key32Bytes = Base64.getEncoder().encodeToString(new byte[32]);
        var props = new CryptoProperties("v1", key32Bytes);
        var service = new MfaCryptoService(props);

        assertNull(service.decrypt(null));
    }

    @Test
    void decrypt_withPlaintextFallback_returnsUnchanged() {
        var key32Bytes = Base64.getEncoder().encodeToString(new byte[32]);
        var props = new CryptoProperties("v1", key32Bytes);
        var service = new MfaCryptoService(props);

        var plaintext = "not-encrypted-value";
        assertEquals(plaintext, service.decrypt(plaintext), "Should return plaintext unchanged");
    }

    @Test
    void constructor_withInvalidKeyLength_throwsException() {
        var invalidKey = Base64.getEncoder().encodeToString(new byte[16]); // Only 16 bytes, need 32
        var props = new CryptoProperties("v1", invalidKey);

        var exception = assertThrows(IllegalArgumentException.class, () -> new MfaCryptoService(props));
        assertTrue(exception.getMessage().contains("32 bytes"));
    }

    @Test
    void isEncrypted_detectsEncryptedValues() {
        var key32Bytes = Base64.getEncoder().encodeToString(new byte[32]);
        var props = new CryptoProperties("v1", key32Bytes);
        var service = new MfaCryptoService(props);

        assertTrue(service.isEncrypted("enc:v1:someiv:somect"));
        assertFalse(service.isEncrypted("plaintext"));
        assertFalse(service.isEncrypted(null));
    }

    @Test
    void decrypt_withInvalidEnvelope_throwsException() {
        var key32Bytes = Base64.getEncoder().encodeToString(new byte[32]);
        var props = new CryptoProperties("v1", key32Bytes);
        var service = new MfaCryptoService(props);

        assertThrows(IllegalArgumentException.class, () -> service.decrypt("enc:invalid"));
        assertThrows(IllegalArgumentException.class, () -> service.decrypt("enc:v1:only-two-parts"));
    }

    @Test
    void encryptDecrypt_withUnicodeCharacters_preservesData() {
        var key32Bytes = Base64.getEncoder().encodeToString(new byte[32]);
        var props = new CryptoProperties("v1", key32Bytes);
        var service = new MfaCryptoService(props);

        var plaintext = "Hello 世界 🔐 émojis";
        var encrypted = service.encrypt(plaintext);
        var decrypted = service.decrypt(encrypted);

        assertEquals(plaintext, decrypted, "Should preserve Unicode characters");
    }
}
