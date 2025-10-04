package io.fortalis.fortalisauth.entity;

import jakarta.persistence.*;

import java.util.UUID;

import lombok.*;

@Entity
@Table(name = "account_mfa")
@Getter
@Setter
@NoArgsConstructor
public class AccountMfa {
    @Id
    private UUID accountId;

    /**
     * 'TOTP' or 'SMS'
     */
    @Column(nullable = false, length = 8)
    private String type;

    /**
     * Base32 TOTP secret (encrypt at rest in real prod).
     */
    @Column(columnDefinition = "TEXT")
    private String secret;

    /**
     * E.164 phone for SMS MFA
     */
    @Column(name = "phone_e164")
    private String phoneE164;

    private boolean enabled;
}
