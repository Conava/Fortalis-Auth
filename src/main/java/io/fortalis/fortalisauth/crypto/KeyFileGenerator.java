package io.fortalis.fortalisauth.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Slf4j
@Component
public class KeyFileGenerator {

    private static final String PRIVATE_KEY_HEADER = "-----BEGIN PRIVATE KEY-----\n";
    private static final String PRIVATE_KEY_FOOTER = "\n-----END PRIVATE KEY-----\n";
    private static final String PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----\n";
    private static final String PUBLIC_KEY_FOOTER = "\n-----END PUBLIC KEY-----\n";
    private static final int KEY_SIZE = 2048;

    public void ensureKeysExist(Path privateKeyPath, Path publicKeyPath) throws IOException, NoSuchAlgorithmException {
        if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
            log.info("Key files already exist at {} and {}", privateKeyPath, publicKeyPath);
            return;
        }

        log.warn("Key files not found. Generating new RSA key pair...");

        var keyPair = generateKeyPair();
        writeKeyFiles(privateKeyPath, publicKeyPath, keyPair);

        log.info("Successfully generated and saved RSA key pair");
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        var keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(KEY_SIZE);
        return keyGen.generateKeyPair();
    }

    private void writeKeyFiles(Path privateKeyPath, Path publicKeyPath, KeyPair keyPair) throws IOException {
        Files.createDirectories(privateKeyPath.getParent());

        var privateKeyPem = formatPrivateKey(keyPair.getPrivate().getEncoded());
        Files.writeString(privateKeyPath, privateKeyPem);

        var publicKeyPem = formatPublicKey(keyPair.getPublic().getEncoded());
        Files.writeString(publicKeyPath, publicKeyPem);
    }

    private String formatPrivateKey(byte[] encoded) {
        var base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
        return PRIVATE_KEY_HEADER + base64 + PRIVATE_KEY_FOOTER;
    }

    private String formatPublicKey(byte[] encoded) {
        var base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
        return PUBLIC_KEY_HEADER + base64 + PUBLIC_KEY_FOOTER;
    }
}
