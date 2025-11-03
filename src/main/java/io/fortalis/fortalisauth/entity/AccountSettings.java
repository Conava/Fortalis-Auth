package io.fortalis.fortalisauth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

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

    private String lastServerName;

    private boolean marketingOptIn;

    private boolean newsletterOptIn;
}
