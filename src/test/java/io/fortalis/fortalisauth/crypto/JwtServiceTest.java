package io.fortalis.fortalisauth.crypto;

import com.nimbusds.jwt.SignedJWT;
import io.fortalis.fortalisauth.config.AuthJwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private AuthJwtProperties properties;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        // Generate test keys
        var keyGenerator = new KeyFileGenerator();
        var privateKeyPath = tempDir.resolve("test_private.pem");
        var publicKeyPath = tempDir.resolve("test_public.pem");
        keyGenerator.ensureKeysExist(privateKeyPath, publicKeyPath);

        // Configure properties
        properties = new AuthJwtProperties();
        properties.setIssuer("test-issuer");
        properties.setAccessTtl(Duration.ofMinutes(15));
        properties.setRefreshTtl(Duration.ofDays(30));
        properties.setKeyFilePrivate(privateKeyPath.toString());
        properties.setKeyFilePublic(publicKeyPath.toString());

        var keyProvider = new KeyProvider(properties);
        jwtService = new JwtService(properties, keyProvider);
    }

    @Test
    void createAccessToken_generatesValidJwt() {
        var accountId = UUID.randomUUID();
        var mfa = true;

        var token = jwtService.createAccessToken(accountId, mfa);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length, "JWT should have 3 parts");
    }

    @Test
    void createAccessToken_containsCorrectClaims() throws Exception {
        var accountId = UUID.randomUUID();
        var mfa = true;

        var token = jwtService.createAccessToken(accountId, mfa);
        var jwt = SignedJWT.parse(token);
        var claims = jwt.getJWTClaimsSet();

        assertEquals("test-issuer", claims.getIssuer());
        assertEquals(accountId.toString(), claims.getSubject());
        assertEquals("fortalis-game", claims.getAudience().getFirst());
        assertTrue((Boolean) claims.getClaim("mfa"));
        assertNotNull(claims.getIssueTime());
        assertNotNull(claims.getExpirationTime());
    }

    @Test
    void createAccessToken_withMfaFalse_setsMfaClaimToFalse() throws Exception {
        var accountId = UUID.randomUUID();
        var token = jwtService.createAccessToken(accountId, false);
        
        var jwt = SignedJWT.parse(token);
        var claims = jwt.getJWTClaimsSet();
        
        assertFalse((Boolean) claims.getClaim("mfa"));
    }

    @Test
    void createAccessToken_setsCorrectExpiration() throws Exception {
        var accountId = UUID.randomUUID();
        var beforeCreation = System.currentTimeMillis();
        
        var token = jwtService.createAccessToken(accountId, true);
        
        var afterCreation = System.currentTimeMillis();
        var jwt = SignedJWT.parse(token);
        var claims = jwt.getJWTClaimsSet();
        
        var actualExpiry = claims.getExpirationTime().getTime();
        var expectedMinExpiry = beforeCreation + properties.getAccessTtl().toMillis() - 1000; // 1 second tolerance
        var expectedMaxExpiry = afterCreation + properties.getAccessTtl().toMillis() + 1000; // 1 second tolerance

        assertTrue(actualExpiry >= expectedMinExpiry,
                "Expiry should be at least " + expectedMinExpiry + " but was " + actualExpiry);
        assertTrue(actualExpiry <= expectedMaxExpiry,
                "Expiry should be at most " + expectedMaxExpiry + " but was " + actualExpiry);
    }

    @Test
    void createAccessToken_signsWithRS256() throws Exception {
        var accountId = UUID.randomUUID();
        var token = jwtService.createAccessToken(accountId, true);
        
        var jwt = SignedJWT.parse(token);
        assertEquals("RS256", jwt.getHeader().getAlgorithm().getName());
    }

    @Test
    void createAccessToken_includesKeyId() throws Exception {
        var accountId = UUID.randomUUID();
        var token = jwtService.createAccessToken(accountId, true);
        
        var jwt = SignedJWT.parse(token);
        var kid = jwt.getHeader().getKeyID();
        
        assertNotNull(kid);
        assertFalse(kid.isEmpty());
    }

    @Test
    void createAccessToken_generatesUniqueTokens() throws Exception {
        var accountId = UUID.randomUUID();
        
        var token1 = jwtService.createAccessToken(accountId, true);
        Thread.sleep(1000); // Wait 1 second to ensure different timestamps
        var token2 = jwtService.createAccessToken(accountId, true);
        
        assertNotEquals(token1, token2, "Each token should be unique due to different issue times");
    }

    @Test
    void createAccessToken_handlesMultipleAccounts() throws Exception {
        var accountId1 = UUID.randomUUID();
        var accountId2 = UUID.randomUUID();
        
        var token1 = jwtService.createAccessToken(accountId1, true);
        var token2 = jwtService.createAccessToken(accountId2, false);
        
        var jwt1 = SignedJWT.parse(token1);
        var jwt2 = SignedJWT.parse(token2);
        
        assertEquals(accountId1.toString(), jwt1.getJWTClaimsSet().getSubject());
        assertEquals(accountId2.toString(), jwt2.getJWTClaimsSet().getSubject());
        assertTrue((Boolean) jwt1.getJWTClaimsSet().getClaim("mfa"));
        assertFalse((Boolean) jwt2.getJWTClaimsSet().getClaim("mfa"));
    }

    @Test
    void jwkSet_returnsPublicKey() {
        var jwkSet = jwtService.jwkSet();
        
        assertNotNull(jwkSet);
        assertFalse(jwkSet.getKeys().isEmpty());
        assertEquals(1, jwkSet.getKeys().size());
    }

    @Test
    void jwkSet_containsOnlyPublicKey() {
        var jwkSet = jwtService.jwkSet();
        var jwk = jwkSet.getKeys().getFirst();
        
        assertTrue(!jwk.isPrivate() || jwk.toJSONObject().get("d") == null,
                "JWK should not contain private key material");
    }

    @Test
    void jwkSet_hasCorrectKeyUsage() {
        var jwkSet = jwtService.jwkSet();
        var jwk = jwkSet.getKeys().getFirst();
        
        assertEquals("sig", jwk.getKeyUse().getValue());
        assertEquals("RS256", jwk.getAlgorithm().getName());
    }

    @Test
    void jwkSet_hasKeyId() {
        var jwkSet = jwtService.jwkSet();
        var jwk = jwkSet.getKeys().getFirst();
        
        assertNotNull(jwk.getKeyID());
        assertFalse(jwk.getKeyID().isEmpty());
    }

    @Test
    void createAccessToken_tokenTypeIsJWT() throws Exception {
        var accountId = UUID.randomUUID();
        var token = jwtService.createAccessToken(accountId, true);
        
        var jwt = SignedJWT.parse(token);
        assertEquals("JWT", jwt.getHeader().getType().toString());
    }
}
