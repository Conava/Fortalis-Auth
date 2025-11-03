package io.fortalis.fortalisauth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "player_server", uniqueConstraints = {
    @UniqueConstraint(name = "uq_account_server", columnNames = {"account_id", "server_id"})
})
@Getter
@Setter
public class PlayerServer {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID accountId;

    @Column(length = 32, nullable = false)
    private String serverId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdTs;

    private Instant lastLoginTs;
}
