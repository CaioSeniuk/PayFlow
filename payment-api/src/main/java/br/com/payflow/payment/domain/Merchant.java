package br.com.payflow.payment.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Merchant(UUID id, String name, String apiKeyHash, Instant createdAt) {

    public Merchant {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(createdAt, "createdAt");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Merchant name must not be blank");
        }
        if (apiKeyHash == null || !apiKeyHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("API key hash must be a SHA-256 hexadecimal digest");
        }
    }
}
