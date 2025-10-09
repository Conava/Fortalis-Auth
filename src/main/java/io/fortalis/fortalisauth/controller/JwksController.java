package io.fortalis.fortalisauth.controller;

import com.nimbusds.jose.jwk.JWKSet;
import io.fortalis.fortalisauth.crypto.JwtService;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes JWKS for public verification by game servers and clients.
 * GET /.well-known/jwks.json
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class JwksController {
    private final JwtService jwt;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        log.debug("JWKS request");
        JWKSet set = jwt.jwkSet();
        return set.toJSONObject(true);
    }
}
