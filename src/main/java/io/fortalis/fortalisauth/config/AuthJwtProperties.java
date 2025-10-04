package io.fortalis.fortalisauth.config;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly-typed binding for auth.jwt.* properties.
 * issuer:     JWT issuer claim and OIDC issuer.
 * accessTtl:  Access token lifetime (short).
 * refreshTtl: Refresh token lifetime (longer).
 * keyStore:   Where to load keys from: files|db|env (files for dev).
 */
@Setter
@Getter
@Validated
@ConfigurationProperties(prefix = "auth.jwt")
public class AuthJwtProperties {
    private String issuer;
    private Duration accessTtl = Duration.ofMinutes(15);
    private Duration refreshTtl = Duration.ofDays(30);
    private String keyStore = "files";
    private String keyFilePrivate;
    private String keyFilePublic;

}
