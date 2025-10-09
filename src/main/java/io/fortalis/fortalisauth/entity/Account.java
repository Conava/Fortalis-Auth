package io.fortalis.fortalisauth.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class Account {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, length = 255)
    private String email;

    private String passwordHash;
    private boolean emailVerified;
    private Instant createdTs;
    private String displayName;

    @PrePersist
    void prePersist() {
        if (createdTs == null) createdTs = Instant.now();
        if (email != null) email = email.toLowerCase(); // normalize to lowercase
    }

    @PreUpdate
    void preUpdate() {
        if (email != null) email = email.toLowerCase();
    }
}

