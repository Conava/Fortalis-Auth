package io.fortalis.fortalisauth.crypto;

import io.fortalis.fortalisauth.config.AuthJwtProperties;

import java.nio.file.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.util.Base64;

import org.springframework.stereotype.Component;

/**
 * Loads RSA keys from files for signing/verification (dev/test).
 * In prod, replace with KMS / Secrets Manager backed provider.
 */
@Component
public class KeyProvider {
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    public KeyProvider(AuthJwtProperties props) {
        try {
            this.privateKey = loadPrivateKey(Paths.get(props.getKeyFilePrivate()));
            this.publicKey = loadPublicKey(Paths.get(props.getKeyFilePublic()));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT keys: " + e.getMessage(), e);
        }
    }

    public RSAPrivateKey privateKey() {
        return privateKey;
    }

    public RSAPublicKey publicKey() {
        return publicKey;
    }

    private static RSAPrivateKey loadPrivateKey(Path pemPath) throws Exception {
        String pem = Files.readString(pemPath)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] der = Base64.getDecoder().decode(pem);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        return (RSAPrivateKey) kf.generatePrivate(spec);
    }

    private static RSAPublicKey loadPublicKey(Path pemPath) throws Exception {
        String pem = Files.readString(pemPath)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(pem);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        return (RSAPublicKey) kf.generatePublic(spec);
    }
}
