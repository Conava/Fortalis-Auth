package io.fortalis.fortalisauth.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

import lombok.*;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class Account {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private boolean emailVerified;

    @Column(nullable = false)
    private Instant createdTs;

    private String displayName;

    @PrePersist
    void prePersist() {
        if (createdTs == null) createdTs = Instant.now();
        if (email != null) email = email.toLowerCase();
    }

    @PreUpdate
    void preUpdate() {
        if (email != null) email = email.toLowerCase();
    }
}