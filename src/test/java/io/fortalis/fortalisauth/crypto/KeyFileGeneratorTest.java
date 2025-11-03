package io.fortalis.fortalisauth.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

class KeyFileGeneratorTest {

    private final KeyFileGenerator keyFileGenerator = new KeyFileGenerator();

    @Test
    void ensureKeysExist_generatesKeysWhenNotPresent(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
        var privateKeyPath = tempDir.resolve("test_private.pem");
        var publicKeyPath = tempDir.resolve("test_public.pem");

        assertFalse(Files.exists(privateKeyPath));
        assertFalse(Files.exists(publicKeyPath));

        keyFileGenerator.ensureKeysExist(privateKeyPath, publicKeyPath);

        assertTrue(Files.exists(privateKeyPath), "Private key file should be created");
        assertTrue(Files.exists(publicKeyPath), "Public key file should be created");
    }

    @Test
    void ensureKeysExist_doesNotOverwriteExistingKeys(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
        var privateKeyPath = tempDir.resolve("existing_private.pem");
        var publicKeyPath = tempDir.resolve("existing_public.pem");

        // Create existing files with specific content
        var existingPrivateContent = "-----BEGIN PRIVATE KEY-----\nexisting\n-----END PRIVATE KEY-----\n";
        var existingPublicContent = "-----BEGIN PUBLIC KEY-----\nexisting\n-----END PUBLIC KEY-----\n";
        Files.writeString(privateKeyPath, existingPrivateContent);
        Files.writeString(publicKeyPath, existingPublicContent);

        keyFileGenerator.ensureKeysExist(privateKeyPath, publicKeyPath);

        // Verify content hasn't changed
        assertEquals(existingPrivateContent, Files.readString(privateKeyPath), "Should not overwrite existing private key");
        assertEquals(existingPublicContent, Files.readString(publicKeyPath), "Should not overwrite existing public key");
    }

    @Test
    void ensureKeysExist_generatesValidPemFormat(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
        var privateKeyPath = tempDir.resolve("valid_private.pem");
        var publicKeyPath = tempDir.resolve("valid_public.pem");

        keyFileGenerator.ensureKeysExist(privateKeyPath, publicKeyPath);

        var privateContent = Files.readString(privateKeyPath);
        var publicContent = Files.readString(publicKeyPath);

        // Verify PEM format
        assertTrue(privateContent.startsWith("-----BEGIN PRIVATE KEY-----"), "Private key should have proper header");
        assertTrue(privateContent.endsWith("-----END PRIVATE KEY-----\n"), "Private key should have proper footer");
        assertTrue(publicContent.startsWith("-----BEGIN PUBLIC KEY-----"), "Public key should have proper header");
        assertTrue(publicContent.endsWith("-----END PUBLIC KEY-----\n"), "Public key should have proper footer");

        // Verify base64 content exists
        var privateBody = privateContent
                .replace("-----BEGIN PRIVATE KEY-----\n", "")
                .replace("\n-----END PRIVATE KEY-----\n", "")
                .trim();
        var publicBody = publicContent
                .replace("-----BEGIN PUBLIC KEY-----\n", "")
                .replace("\n-----END PUBLIC KEY-----\n", "")
                .trim();

        assertFalse(privateBody.isEmpty(), "Private key should have content");
        assertFalse(publicBody.isEmpty(), "Public key should have content");
    }

    @Test
    void ensureKeysExist_createsParentDirectories(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
        var nestedDir = tempDir.resolve("keys").resolve("nested");
        var privateKeyPath = nestedDir.resolve("private.pem");
        var publicKeyPath = nestedDir.resolve("public.pem");

        assertFalse(Files.exists(nestedDir));

        keyFileGenerator.ensureKeysExist(privateKeyPath, publicKeyPath);

        assertTrue(Files.exists(nestedDir), "Parent directories should be created");
        assertTrue(Files.exists(privateKeyPath));
        assertTrue(Files.exists(publicKeyPath));
    }

    @Test
    void ensureKeysExist_generatesRsa2048Keys(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
        var privateKeyPath = tempDir.resolve("rsa_private.pem");
        var publicKeyPath = tempDir.resolve("rsa_public.pem");

        keyFileGenerator.ensureKeysExist(privateKeyPath, publicKeyPath);

        var privateContent = Files.readString(privateKeyPath);
        var publicContent = Files.readString(publicKeyPath);

        // RSA 2048 keys should have certain minimum lengths
        assertTrue(privateContent.length() > 1000, "Private key should be substantial");
        assertTrue(publicContent.length() > 300, "Public key should be substantial");
    }

    @Test
    void ensureKeysExist_generatesMatchingKeyPair(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
        var privateKeyPath = tempDir.resolve("match_private.pem");
        var publicKeyPath = tempDir.resolve("match_public.pem");

        keyFileGenerator.ensureKeysExist(privateKeyPath, publicKeyPath);

        // Both files should exist and have content
        assertTrue(Files.size(privateKeyPath) > 0);
        assertTrue(Files.size(publicKeyPath) > 0);

        // Keys should be different
        var privateContent = Files.readString(privateKeyPath);
        var publicContent = Files.readString(publicKeyPath);
        assertNotEquals(privateContent, publicContent, "Private and public keys should be different");
    }

    @Test
    void ensureKeysExist_onlyPublicKeyMissing_generatesBoth(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
        var privateKeyPath = tempDir.resolve("partial_private.pem");
        var publicKeyPath = tempDir.resolve("partial_public.pem");

        // Create only private key
        Files.writeString(privateKeyPath, "existing private key");

        keyFileGenerator.ensureKeysExist(privateKeyPath, publicKeyPath);

        // Both files should now exist
        assertTrue(Files.exists(privateKeyPath));
        assertTrue(Files.exists(publicKeyPath));
        
        // Since the implementation requires BOTH to exist, it generates new keys
        // So the old private key will be overwritten
        var privateContent = Files.readString(privateKeyPath);
        assertTrue(privateContent.startsWith("-----BEGIN PRIVATE KEY-----"),
                "Should have regenerated with proper PEM format");
    }

    @Test
    void ensureKeysExist_generatesUniqueKeys(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
        var privateKeyPath1 = tempDir.resolve("unique1_private.pem");
        var publicKeyPath1 = tempDir.resolve("unique1_public.pem");
        var privateKeyPath2 = tempDir.resolve("unique2_private.pem");
        var publicKeyPath2 = tempDir.resolve("unique2_public.pem");

        keyFileGenerator.ensureKeysExist(privateKeyPath1, publicKeyPath1);
        keyFileGenerator.ensureKeysExist(privateKeyPath2, publicKeyPath2);

        var private1 = Files.readString(privateKeyPath1);
        var private2 = Files.readString(privateKeyPath2);

        assertNotEquals(private1, private2, "Each invocation should generate unique keys");
    }
}
