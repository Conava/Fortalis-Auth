package io.fortalis.fortalisauth.entity;

import jakarta.persistence.*;

import java.util.UUID;

import lombok.*;

@Entity
@Table(name = "account_identity",
        uniqueConstraints = @UniqueConstraint(name = "uk_provider_subject", columnNames = {"provider", "subject"}))
@Getter
@Setter
@NoArgsConstructor
public class AccountIdentity {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    /**
     * 'password' | 'google' | 'apple'
     */
    @Column(nullable = false, length = 16)
    private String provider;

    /**
     * OIDC sub or local identity key
     */
    @Column(nullable = false, length = 255)
    private String subject;
}
