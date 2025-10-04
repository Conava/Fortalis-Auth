package io.fortalis.fortalisauth.crypto;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.fortalis.fortalisauth.config.AuthJwtProperties;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

import org.springframework.stereotype.Service;

/**
 * Issues RS256-signed JWTs and exposes a JWK (public) for verification by game servers.
 */
@Service
public class JwtService {
    private final AuthJwtProperties props;
    private final KeyProvider keyProvider;
    private final JWK jwk;

    public JwtService(AuthJwtProperties props, KeyProvider keyProvider) {
        this.props = props;
        this.keyProvider = keyProvider;
        // Build a JWK with a random kid so JWKS caches can refresh when key changes.
        String kid = randomKid();
        this.jwk = new RSAKey.Builder(keyProvider.publicKey())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(kid)
                .build();
    }

    /**
     * Returns a JWKSet containing the current public key.
     */
    public JWKSet jwkSet() {
        return new JWKSet(jwk.toPublicJWK());
    }

    /**
     * Creates an access token with standard claims.
     */
    public String createAccessToken(UUID accountId, boolean mfa) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.getAccessTtl());

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(props.getIssuer())
                .subject(accountId.toString())
                .audience("fortalis-game")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .claim("mfa", mfa)
                .build();

        return sign(claims);
    }

    private String sign(JWTClaimsSet claims) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(jwk.getKeyID())
                    .type(JOSEObjectType.JWT)
                    .build();
            SignedJWT jwt = new SignedJWT(header, claims);
            RSASSASigner signer = new RSASSASigner(keyProvider.privateKey());
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("JWT signing failed", e);
        }
    }

    private static String randomKid() {
        byte[] buf = new byte[8];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
