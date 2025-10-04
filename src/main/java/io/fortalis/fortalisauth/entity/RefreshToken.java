package io.fortalis.fortalisauth.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

import lombok.*;

@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "idx_refresh_account", columnList = "account_id")
})
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    /**
     * Store a hash of the token, not the token itself.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String tokenHash;

    private Instant issuedTs;

    private Instant expiresTs;

    private String userAgent;

    private String deviceLabel;

    private boolean revoked;

    @PrePersist
    void prePersist() {
        if (issuedTs == null) issuedTs = Instant.now();
    }
}
