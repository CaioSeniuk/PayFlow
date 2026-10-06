package br.com.payflow.payment.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

import br.com.payflow.payment.domain.Merchant;

public final class ProvisionDemoMerchant {

    public static final UUID MERCHANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final MerchantStore merchants;
    private final Clock clock;

    public ProvisionDemoMerchant(MerchantStore merchants, Clock clock) {
        this.merchants = Objects.requireNonNull(merchants);
        this.clock = Objects.requireNonNull(clock);
    }

    public Merchant execute(String apiKey) {
        if (apiKey == null || apiKey.isBlank() || apiKey.length() < 32 || apiKey.length() > 256) {
            throw new IllegalArgumentException("Demo API key must contain between 32 and 256 characters");
        }
        String hash = hashApiKey(apiKey);
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

    private static String hashApiKey(String apiKey) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(apiKey.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
