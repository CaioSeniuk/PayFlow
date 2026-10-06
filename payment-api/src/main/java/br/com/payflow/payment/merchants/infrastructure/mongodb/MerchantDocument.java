package br.com.payflow.payment.merchants.infrastructure.mongodb;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("merchants")
public class MerchantDocument {

    @Id
    private UUID id;

    private String name;

    private String apiKeyHash;

    private Instant createdAt;

    protected MerchantDocument() {
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getApiKeyHash() {
        return apiKeyHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
