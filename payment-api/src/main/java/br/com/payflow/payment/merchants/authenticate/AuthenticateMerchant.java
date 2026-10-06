package br.com.payflow.payment.merchants.authenticate;

import java.util.Objects;

import br.com.payflow.payment.merchants.domain.ApiKey;
import br.com.payflow.payment.merchants.domain.ApiKeyHasher;

public final class AuthenticateMerchant {

    private final MerchantLookup merchants;
    private final ApiKeyHasher hasher;

    public AuthenticateMerchant(MerchantLookup merchants, ApiKeyHasher hasher) {
        this.merchants = Objects.requireNonNull(merchants);
        this.hasher = Objects.requireNonNull(hasher);
    }

    public AuthenticatedMerchant execute(String apiKey) {
        if (!ApiKey.isWellFormed(apiKey)) {
            throw new InvalidApiKeyException();
        }
        return merchants.findByApiKeyHash(hasher.hash(ApiKey.of(apiKey)))
                .map(merchant -> new AuthenticatedMerchant(merchant.id(), merchant.name()))
                .orElseThrow(InvalidApiKeyException::new);
    }
}
