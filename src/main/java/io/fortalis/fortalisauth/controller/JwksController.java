package io.fortalis.fortalisauth.controller;

import com.nimbusds.jose.jwk.JWKSet;
import io.fortalis.fortalisauth.crypto.JwtService;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes JWKS for public verification by game servers and clients.
 * GET /.well-known/jwks.json
 */
@RestController
@RequiredArgsConstructor
public class JwksController {
    private final JwtService jwt;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        JWKSet set = jwt.jwkSet();
        return set.toJSONObject(true);
    }
}
