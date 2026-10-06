package br.com.payflow.payment.merchants.provision;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import br.com.payflow.payment.merchants.domain.ApiKey;
import br.com.payflow.payment.merchants.domain.ApiKeyHasher;
import br.com.payflow.payment.merchants.domain.Merchant;

public final class ProvisionDemoMerchant {

    public static final UUID MERCHANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final MerchantProvisioning merchants;
    private final ApiKeyHasher hasher;
    private final Clock clock;

    public ProvisionDemoMerchant(MerchantProvisioning merchants, ApiKeyHasher hasher, Clock clock) {
        this.merchants = Objects.requireNonNull(merchants);
        this.hasher = Objects.requireNonNull(hasher);
        this.clock = Objects.requireNonNull(clock);
    }

    public Merchant execute(String apiKey) {
        String hash = hasher.hash(ApiKey.of(apiKey));
        Merchant stored = Objects.requireNonNull(merchants.insertIfAbsent(
                new Merchant(MERCHANT_ID, "Demo Merchant", hash, Instant.now(clock))),
                "Merchant store returned no merchant");
        if (!MERCHANT_ID.equals(stored.id()) || !hash.equals(stored.apiKeyHash())) {
            throw new IllegalStateException(
                    "Demo merchant already exists with a different API key. "
                    + "Use the original key; changing the environment does not rotate credentials.");
        }
        return stored;
    }
}
