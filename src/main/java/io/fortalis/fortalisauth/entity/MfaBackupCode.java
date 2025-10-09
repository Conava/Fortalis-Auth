package io.fortalis.fortalisauth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mfa_backup_code")
@Getter
@Setter
@NoArgsConstructor
public class MfaBackupCode {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String codeHash;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private Instant createdTs;

    @PrePersist
    void prePersist() {
        if (createdTs == null) createdTs = Instant.now();
    }
}

