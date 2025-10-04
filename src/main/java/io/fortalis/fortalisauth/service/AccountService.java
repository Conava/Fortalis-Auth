package io.fortalis.fortalisauth.service;

import io.fortalis.fortalisauth.entity.*;
import io.fortalis.fortalisauth.repo.*;
import io.fortalis.fortalisauth.web.ApiException;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account lifecycle: register, lookup, password hashing/verify.
 */
@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accounts;
    private final AccountIdentityRepository identities;

    private final Argon2PasswordEncoder encoder =
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    @Transactional
    public Account register(String email, String rawPassword, String displayName) {
        accounts.findByEmail(email).ifPresent(a -> {
            throw ApiException.badRequest("email_taken", "Email already registered.");
        });

        Account a = new Account();
        a.setEmail(email);
        a.setPasswordHash(encoder.encode(rawPassword));
        a.setDisplayName(displayName);
        a.setEmailVerified(false);
        a = accounts.save(a);

        AccountIdentity id = new AccountIdentity();
        id.setAccount(a);
        id.setProvider("password");
        id.setSubject(email.toLowerCase());
        identities.save(id);

        return a;
    }

    public Optional<Account> findByEmailOrUsername(String input) {
        // For now treat it as email; you can add a username lookup later.
        return accounts.findByEmail(input);
    }

    public boolean matches(String raw, String encoded) {
        return encoder.matches(raw, encoded);
    }
}
