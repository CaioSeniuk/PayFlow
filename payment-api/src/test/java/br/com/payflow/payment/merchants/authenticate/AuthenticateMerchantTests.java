package br.com.payflow.payment.merchants.authenticate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import br.com.payflow.payment.merchants.domain.Merchant;
import br.com.payflow.payment.merchants.infrastructure.crypto.Sha256ApiKeyHasher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticateMerchantTests {

    private static final String KEY = "a".repeat(32);
    private static final String KEY_HASH = "3ba3f5f43b92602683c19aee62a20342b084dd5971ddd33808d81a328879a547";
    private static final Merchant MERCHANT = new Merchant(
            UUID.fromString("00000000-0000-0000-0000-000000000001"), "Demo Merchant", KEY_HASH,
            Instant.parse("2026-10-06T00:00:00Z"));

    private final AtomicInteger lookups = new AtomicInteger();
    private final AuthenticateMerchant useCase = new AuthenticateMerchant(hash -> {
        lookups.incrementAndGet();
        return KEY_HASH.equals(hash) ? Optional.of(MERCHANT) : Optional.empty();
    }, new Sha256ApiKeyHasher());

    @Test
    void knownKeyReturnsMerchantIdentityWithoutCredentials() {
        AuthenticatedMerchant merchant = useCase.execute(KEY);
        assertThat(merchant.id()).isEqualTo(MERCHANT.id());
        assertThat(merchant.name()).isEqualTo("Demo Merchant");
        assertThat(merchant.toString()).doesNotContain(KEY).doesNotContain(KEY_HASH);
    }

    @Test
    void unknownKeyIsRejected() {
        assertThatThrownBy(() -> useCase.execute("b".repeat(32)))
                .isInstanceOf(InvalidApiKeyException.class);
        assertThat(lookups).hasValue(1);
    }

    @Test
    void malformedKeyIsRejectedWithoutQueryingTheStore() {
        for (String key : new String[]{null, "", " ".repeat(32), "short", "a".repeat(257)}) {
            assertThatThrownBy(() -> useCase.execute(key)).isInstanceOf(InvalidApiKeyException.class);
        }
        assertThat(lookups).hasValue(0);
    }

    @Test
    void storeFailureIsNotReportedAsInvalidKey() {
        var failing = new AuthenticateMerchant(hash -> {
            throw new IllegalStateException("Storage unavailable");
        }, new Sha256ApiKeyHasher());
        assertThatThrownBy(() -> failing.execute(KEY))
                .isInstanceOf(IllegalStateException.class).hasMessage("Storage unavailable");
    }
}
