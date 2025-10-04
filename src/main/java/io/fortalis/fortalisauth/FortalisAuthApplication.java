package io.fortalis.fortalisauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for Fortalis Auth service.
 * This service issues JWTs, manages accounts, MFA, and exposes JWKS for verification.
 */
@SpringBootApplication
public class FortalisAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(FortalisAuthApplication.class, args);
    }
}
