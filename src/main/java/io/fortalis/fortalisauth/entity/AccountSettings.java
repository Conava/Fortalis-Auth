package io.fortalis.fortalisauth.entity;

import jakarta.persistence.*;

import java.util.UUID;

import lombok.*;

@Entity
@Table(name = "account_settings")
@Getter
@Setter
@NoArgsConstructor
public class AccountSettings {
    @Id
    private UUID accountId;

    @Column(nullable = false, length = 8)
    private String lang = "en";

    private boolean marketingOptIn;
}
